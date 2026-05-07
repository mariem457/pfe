import math
from collections import Counter

from ortools.constraint_solver import pywrapcp, routing_enums_pb2

from config.settings import (
    DEFAULT_MAX_ROUTE_MINUTES,
    OPPORTUNISTIC_MAX_DISTANCE_FROM_MANDATORY_METERS,
    OPPORTUNISTIC_MAX_DISTANCE_FROM_DEPOT_METERS,
    OPPORTUNISTIC_HIGH_SCORE_BYPASS,
)
from models.routing_models import (
    RouteCoordinateDto,
    RoutingMissionDto,
    RoutingRequestDto,
    RoutingResponseDto,
    RoutingStopDto,
)
from services.incident_service import filter_eligible_trucks
from services.matrix_service import create_matrices
from services.osrm_service import call_osrm_route, call_osrm_route_summary
from services.tomtom_service import call_tomtom_route

DEFAULT_BIN_DROPOUT_PENALTY = 10000
MANDATORY_BASE_PENALTY = 100000
OPPORTUNISTIC_BASE_PENALTY = 30000
REPORTABLE_BASE_PENALTY = 8000

MORNING_RUN_MAX_MINUTES = 9 * 60
EVENING_RUN_MAX_MINUTES = 9 * 60

DEFAULT_SERVICE_TIME_MINUTES = 2
DISPOSAL_SERVICE_TIME_MINUTES = 5

MORNING_START_MINUTE_OF_DAY = 14 * 60
EVENING_START_MINUTE_OF_DAY = 14 * 60

MAX_PASS2_BINS = 90
DISPOSAL_LOAD_RATIO_THRESHOLD = 0.75
MIN_BINS_AFTER_DISPOSAL = 2
URGENT_DISPOSAL_LOAD_RATIO_THRESHOLD = 0.50

def is_bin_urgent(bin_dto) -> bool:
    predicted_hours = getattr(bin_dto, "predictedHoursToFull", None)
    priority = float(getattr(bin_dto, "predictedPriority", 0.0) or 0.0)
    fill_level = float(getattr(bin_dto, "fillLevel", 0.0) or 0.0)

    if predicted_hours is not None and predicted_hours <= 3:
        return True

    if priority >= 0.9:
        return True

    if fill_level >= 95:
        return True

    return False

def normalize_text(value) -> str:
    if value is None:
        return ""
    return str(value).strip().upper()




def waste_family(waste_type: str | None) -> str:
    wt = normalize_text(waste_type)

    if wt in {"GRAY", "GREY", "GREEN"}:
        return "ORGANIC"

    if wt == "YELLOW":
        return "YELLOW"

    if wt == "WHITE":
        return "WHITE"

    return "UNKNOWN"


def split_bins_by_waste_family(bins):
    grouped = {}

    for b in bins or []:
        family = waste_family(getattr(b, "wasteType", None))
        grouped.setdefault(family, []).append(b)

    return grouped


def build_family_request(original_request: RoutingRequestDto, selected_bins) -> RoutingRequestDto:
    return RoutingRequestDto(
        depot=original_request.depot,
        trafficMode=original_request.trafficMode,
        currentRun=original_request.currentRun,
        bins=selected_bins,
        trucks=original_request.trucks,
        disposalSites=original_request.disposalSites,
        activeIncidents=original_request.activeIncidents,
    )


def merge_family_responses(responses, all_input_bin_ids):
    missions = []
    excluded_trucks = []
    warning_trucks = []
    recommended_fuel_stations = []
    served_bin_ids = set()
    matrix_sources = []

    for response in responses:
        if response is None:
            continue

        missions.extend(response.missions or [])
        excluded_trucks.extend(response.excludedTrucks or [])
        warning_trucks.extend(response.warningTrucks or [])
        recommended_fuel_stations.extend(response.recommendedFuelStations or [])

        if response.matrixSource:
            matrix_sources.append(response.matrixSource)

        for mission in response.missions or []:
            for stop in mission.stops or []:
                if stop.stopType == "BIN_PICKUP" and stop.binId is not None:
                    served_bin_ids.add(stop.binId)

    dropped_bin_ids = sorted(set(all_input_bin_ids) - served_bin_ids)

    return RoutingResponseDto(
        missions=missions,
        matrixSource="+".join(sorted(set(matrix_sources))) if matrix_sources else "NONE",
        excludedTrucks=excluded_trucks,
        warningTrucks=warning_trucks,
        recommendedFuelStations=recommended_fuel_stations,
        droppedBinIds=dropped_bin_ids,
    )



def haversine_meters(lat1, lng1, lat2, lng2) -> int:
    r = 6371000.0
    dlat = math.radians(lat2 - lat1)
    dlng = math.radians(lng2 - lng1)
    a = (
        math.sin(dlat / 2) ** 2
        + math.cos(math.radians(lat1))
        * math.cos(math.radians(lat2))
        * math.sin(dlng / 2) ** 2
    )
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return int(r * c)

def cluster_soft_penalty(from_bin, to_bin) -> int:
    if from_bin is None or to_bin is None:
        return 0

    from_zone = getattr(from_bin, "zoneId", None)
    to_zone = getattr(to_bin, "zoneId", None)

    from_cluster = getattr(from_bin, "clusterId", None)
    to_cluster = getattr(to_bin, "clusterId", None)

    if from_zone == to_zone and from_cluster == to_cluster:
        return 0

    if from_zone == to_zone and from_cluster != to_cluster:
        return 300

    return 0
def is_truck_compatible_with_bin(truck, bin_dto) -> bool:
    bin_waste_type = normalize_text(getattr(bin_dto, "wasteType", None))
    if not bin_waste_type:
        return True

    supported_types = getattr(truck, "supportedWasteTypes", None)
    if not supported_types:
        return True

    normalized_supported = {normalize_text(w) for w in supported_types if w is not None}
    return bin_waste_type in normalized_supported


def is_truck_spatially_compatible_with_bin(truck, bin_dto) -> bool:
    truck_zone = getattr(truck, "zoneId", None)
    bin_zone = getattr(bin_dto, "zoneId", None)

    if truck_zone is not None and bin_zone is not None and truck_zone != bin_zone:
        return False

    return True

def zone_has_eligible_truck(eligible_trucks, bin_zone) -> bool:
    if bin_zone is None:
        return True

    for truck in eligible_trucks:
        truck_zone = getattr(truck, "zoneId", None)
        if truck_zone == bin_zone:
            return True

    return False


def get_compatible_vehicle_indices_for_bin(eligible_trucks, bin_dto) -> list[int]:
    compatible_vehicle_indices = []

    bin_zone = getattr(bin_dto, "zoneId", None)
    has_truck_in_same_zone = zone_has_eligible_truck(eligible_trucks, bin_zone)

    for vehicle_index, truck in enumerate(eligible_trucks):
        if not is_truck_compatible_with_bin(truck, bin_dto):
            continue

        truck_zone = getattr(truck, "zoneId", None)

        # الحالة العادية: zone hard
        if has_truck_in_same_zone:
            if truck_zone == bin_zone:
                compatible_vehicle_indices.append(int(vehicle_index))

        # emergency fallback: ما فما حتى camion eligible في نفس zone
        # نخلي camions من zones أخرى يخدمو bin
        else:
            compatible_vehicle_indices.append(int(vehicle_index))

    return compatible_vehicle_indices


def normalize_category(bin_dto) -> str:
    if is_bin_urgent(bin_dto):
        return "MANDATORY"

    category = normalize_text(bin_dto.decisionCategory)

    if category in {"MANDATORY", "OPPORTUNISTIC", "REPORTABLE"}:
        return category

    if bool(bin_dto.mandatory):
        return "MANDATORY"

    if bool(bin_dto.opportunistic):
        return "OPPORTUNISTIC"

    if bool(bin_dto.reportable):
        return "REPORTABLE"

    return "UNKNOWN"


def resolve_run_max_minutes(current_run: str | None) -> int:
    run_value = normalize_text(current_run)

    if run_value == "MORNING":
        return MORNING_RUN_MAX_MINUTES

    if run_value == "EVENING":
        return EVENING_RUN_MAX_MINUTES

    return DEFAULT_MAX_ROUTE_MINUTES


def resolve_run_start_minute_of_day(current_run: str | None) -> int:
    run_value = normalize_text(current_run)

    if run_value == "EVENING":
        return EVENING_START_MINUTE_OF_DAY

    return MORNING_START_MINUTE_OF_DAY


def resolve_relative_time_window(bin_dto, current_run: str | None, run_max_minutes: int):
    start_abs = getattr(bin_dto, "windowStartMinutes", None)
    end_abs = getattr(bin_dto, "windowEndMinutes", None)

    if start_abs is None or end_abs is None:
        return None

    start_abs = int(start_abs)
    end_abs = int(end_abs)

    if start_abs > end_abs:
        return None

    run_start_abs = resolve_run_start_minute_of_day(current_run)
    run_end_abs = run_start_abs + run_max_minutes

    intersect_start = max(start_abs, run_start_abs)
    intersect_end = min(end_abs, run_end_abs)

    if intersect_start > intersect_end:
        return None

    start_rel = intersect_start - run_start_abs
    end_rel = intersect_end - run_start_abs

    return start_rel, end_rel
def compute_drop_penalty(bin_dto) -> int:

    if is_bin_urgent(bin_dto):
        return 200000

    category = normalize_category(bin_dto)

    priority = float(bin_dto.predictedPriority or 0.0)
    feedback_score = float(bin_dto.feedbackScore or 0.0)
    postponement_count = int(bin_dto.postponementCount or 0)
    predicted_hours = bin_dto.predictedHoursToFull

    # MANDATORY
    if category == "MANDATORY":
        return 100000

    # OPPORTUNISTIC
    if category == "OPPORTUNISTIC":
        penalty = OPPORTUNISTIC_BASE_PENALTY
        penalty += int(priority * 12000)
        penalty += int(feedback_score * 1500)
        penalty += postponement_count * 800

        if predicted_hours is not None:
            if predicted_hours <= 12:
                penalty += 10000
            elif predicted_hours <= 24:
                penalty += 5000

        return penalty

    # REPORTABLE
    if category == "REPORTABLE":
        penalty = REPORTABLE_BASE_PENALTY
        penalty += int(priority * 5000)
        penalty += int(feedback_score * 500)
        penalty += postponement_count * 300
        return penalty

    # DEFAULT
    base_penalty = DEFAULT_BIN_DROPOUT_PENALTY + int(priority * 20000)

    if predicted_hours is None:
        return base_penalty

    if predicted_hours <= 6:
        return base_penalty + 40000
    if predicted_hours <= 12:
        return base_penalty + 25000
    if predicted_hours <= 24:
        return base_penalty + 10000

    return base_penalty


def split_bins_by_category(bins):
    mandatory_bins = []
    opportunistic_bins = []
    reportable_bins = []

    for b in bins:
        category = normalize_category(b)
        if category == "MANDATORY":
            mandatory_bins.append(b)
        elif category == "OPPORTUNISTIC":
            opportunistic_bins.append(b)
        else:
            reportable_bins.append(b)

    return mandatory_bins, opportunistic_bins, reportable_bins


def sort_opportunistic_bins(opportunistic_bins):
    return sorted(
        opportunistic_bins,
        key=lambda b: (
            float(b.opportunisticScore or 0.0),
            float(b.feedbackScore or 0.0),
            -float(b.predictedHoursToFull or 999999.0),
        ),
        reverse=True,
    )

def filter_opportunistic_by_distance(request: RoutingRequestDto, mandatory_bins, opportunistic_bins):
    if not opportunistic_bins:
        return []

    # إذا ما فماش mandatory، نقيسو opportunistic بالنسبة للـ depot
    reference_points = []

    if mandatory_bins:
        reference_points = [(b.lat, b.lng, f"mandatory:{b.id}") for b in mandatory_bins]
        max_distance = OPPORTUNISTIC_MAX_DISTANCE_FROM_MANDATORY_METERS
    else:
        reference_points = [(request.depot.lat, request.depot.lng, "depot")]
        max_distance = OPPORTUNISTIC_MAX_DISTANCE_FROM_DEPOT_METERS

    filtered = []

    print(
        f"Opportunistic distance filter => candidates={len(opportunistic_bins)}, "
        f"references={len(reference_points)}, maxDistance={max_distance}m",
        flush=True,
    )

    for b in opportunistic_bins:
        score = float(getattr(b, "opportunisticScore", 0.0) or 0.0)

        # score عالي برشا: نخليه حتى لو بعيد
        if score >= OPPORTUNISTIC_HIGH_SCORE_BYPASS:
            filtered.append(b)
            print(
                f"OPPORTUNISTIC KEPT BY SCORE => binId={b.id}, score={score}",
                flush=True,
            )
            continue

        nearest_distance = None
        nearest_ref = None

        for ref_lat, ref_lng, ref_name in reference_points:
            d = haversine_meters(b.lat, b.lng, ref_lat, ref_lng)

            if nearest_distance is None or d < nearest_distance:
                nearest_distance = d
                nearest_ref = ref_name

        if nearest_distance is not None and nearest_distance <= max_distance:
            filtered.append(b)
            print(
                f"OPPORTUNISTIC KEPT BY DISTANCE => binId={b.id}, "
                f"nearest={nearest_ref}, distance={nearest_distance}m, score={score}",
                flush=True,
            )
        else:
            print(
                f"OPPORTUNISTIC DROPPED BY DISTANCE => binId={b.id}, "
                f"nearest={nearest_ref}, distance={nearest_distance}m, "
                f"maxAllowed={max_distance}m, score={score}",
                flush=True,
            )

    print(
        f"Opportunistic distance filter result => kept={len(filtered)}, dropped={len(opportunistic_bins) - len(filtered)}",
        flush=True,
    )

    return filtered


def build_sub_request(original_request: RoutingRequestDto, selected_bins) -> RoutingRequestDto:
    return RoutingRequestDto(
        depot=original_request.depot,
        trafficMode=original_request.trafficMode,
        currentRun=original_request.currentRun,
        bins=selected_bins,
        trucks=original_request.trucks,
        disposalSites=original_request.disposalSites,
        activeIncidents=original_request.activeIncidents,
    )


def is_disposal_compatible(disposal_site, waste_type: str | None) -> bool:
    accepted = getattr(disposal_site, "acceptedWasteTypes", None) or []
    if not accepted:
        return True

    normalized_accepted = {normalize_text(x) for x in accepted}
    wt = normalize_text(waste_type)

    if not wt:
        return True

    return wt in normalized_accepted


def choose_disposal_site(request: RoutingRequestDto, waste_type: str | None, current_point):
    sites = request.disposalSites or []

    compatible_sites = [
        s for s in sites
        if is_disposal_compatible(s, waste_type)
    ]

    if not compatible_sites:
        compatible_sites = sites

    if not compatible_sites:
        return None

    lat, lng = current_point

    return min(
        compatible_sites,
        key=lambda s: haversine_meters(lat, lng, s.lat, s.lng)
    )


def add_disposal_stop_if_needed(
    request: RoutingRequestDto,
    stops,
    ordered_points,
    order: int,
    current_load: float,
    waste_type: str | None,
):
    if current_load <= 0:
        return order, current_load, 0, 0

    last_point = ordered_points[-1]
    disposal = choose_disposal_site(request, waste_type, last_point)

    if disposal is None:
        print("WARNING: No disposal site available. Skipping disposal insertion.", flush=True)
        return order, current_load, 0, 0

    stops.append(
        RoutingStopDto(
            stopType="DISPOSAL_SITE",
            disposalSiteId=disposal.id,
            orderIndex=order,
        )
    )

    order += 1

    try:
        extra_distance, travel_duration = call_osrm_route_summary([
            last_point,
            (disposal.lat, disposal.lng)
        ])
        extra_duration = travel_duration + DISPOSAL_SERVICE_TIME_MINUTES
    except Exception as e:
        print(
            f"OSRM disposal summary failed, fallback to haversine: {e}",
            flush=True
        )
        extra_distance = haversine_meters(
            last_point[0],
            last_point[1],
            disposal.lat,
            disposal.lng
        )
        extra_duration = max(
            1,
            int((extra_distance / 1000 / 30.0) * 60)
        ) + DISPOSAL_SERVICE_TIME_MINUTES

    ordered_points.append((disposal.lat, disposal.lng))

    print(
        f"[DISPOSAL] siteId={disposal.id} | name={getattr(disposal, 'name', None)} | "
        f"loadBefore={round(current_load, 2)}kg",
        flush=True,
    )

    return order, 0, extra_distance, extra_duration






def count_remaining_bins_in_route(solution, routing, manager, start_index) -> int:
    count = 0
    tmp_index = start_index

    while not routing.IsEnd(tmp_index):
        node = manager.IndexToNode(tmp_index)
        if node != 0:
            count += 1
        tmp_index = solution.Value(routing.NextVar(tmp_index))

    return count

def should_insert_disposal_smartly(
    current_load: float,
    capacity: float,
    upcoming_bins_count: int,
    next_bin_is_urgent: bool,
) -> bool:
    if capacity <= 0 or current_load <= 0:
        return False

    load_ratio = current_load / capacity

    # إذا الباك الجاي urgent، نفرغ حتى لو باقي كان باك واحد
    if next_bin_is_urgent and load_ratio >= URGENT_DISPOSAL_LOAD_RATIO_THRESHOLD:
        return True

    # إذا مازال كان باك واحد وماهوش urgent، ما نفرغوش
    if upcoming_bins_count <= 1:
        return False

    if load_ratio >= DISPOSAL_LOAD_RATIO_THRESHOLD and upcoming_bins_count >= MIN_BINS_AFTER_DISPOSAL:
        return True

    return False
def build_response_from_solution(
    request: RoutingRequestDto,
    eligible_trucks,
    excluded_trucks,
    warning_trucks,
    distance_matrix,
    duration_matrix,
    matrix_source,
    solution,
    routing,
    manager,
) -> RoutingResponseDto:
    missions = []
    served_bin_ids = set()

    all_stops_counts = []
    all_durations = []
    all_distances = []

    for v in range(len(eligible_trucks)):
        index = routing.Start(v)
        stops = []
        total_dist = 0
        total_time = 0
        order = 1

        truck = eligible_trucks[v]
        capacity = float(getattr(truck, "remainingCapacityKg", 0) or 0)
        current_load = 0.0
        last_waste_type = None

        ordered_points = [(request.depot.lat, request.depot.lng)]

        while not routing.IsEnd(index):
            next_index = solution.Value(routing.NextVar(index))
            from_node = manager.IndexToNode(index)
            to_node = manager.IndexToNode(next_index)

            if to_node != 0:
                selected_bin = request.bins[to_node - 1]
                bin_load = float(selected_bin.estimatedLoadKg or 0.0)
                bin_waste_type = getattr(selected_bin, "wasteType", None)

                if capacity > 0 and bin_load > capacity:
                    print(
                        f"WARNING: binId={selected_bin.id} load={round(bin_load, 2)}kg exceeds "
                        f"truck capacity={round(capacity, 2)}kg. Skipping bin.",
                        flush=True,
                    )
                    index = next_index
                    continue

                if capacity > 0 and current_load > 0 and current_load + bin_load > capacity:
                    upcoming_bins_count = count_remaining_bins_in_route(
                        solution=solution,
                        routing=routing,
                        manager=manager,
                        start_index=next_index,
                    )

                    next_bin_is_urgent = is_bin_urgent(selected_bin)

                    if should_insert_disposal_smartly(
                        current_load=current_load,
                        capacity=capacity,
                        upcoming_bins_count=upcoming_bins_count,
                        next_bin_is_urgent=next_bin_is_urgent,
                    ):
                        order, current_load, extra_dist, extra_time = add_disposal_stop_if_needed(
                            request=request,
                            stops=stops,
                            ordered_points=ordered_points,
                            order=order,
                            current_load=current_load,
                            waste_type=last_waste_type,
                        )
                        total_dist += extra_dist
                        total_time += extra_time
                    else:
                        print(
                            f"[SMART DISPOSAL] skipped binId={selected_bin.id} because disposal is not efficient "
                            f"| currentLoad={round(current_load, 2)}kg "
                            f"| capacity={round(capacity, 2)}kg "
                            f"| binLoad={round(bin_load, 2)}kg "
                            f"| upcomingBins={upcoming_bins_count}",
                            flush=True,
                        )
                        index = next_index
                        continue

                served_bin_ids.add(selected_bin.id)

                stops.append(
                    RoutingStopDto(
                        stopType="BIN_PICKUP",
                        binId=selected_bin.id,
                        orderIndex=order,
                    )
                )
                order += 1

                ordered_points.append((selected_bin.lat, selected_bin.lng))
                current_load += bin_load
                last_waste_type = bin_waste_type

            total_dist += distance_matrix[from_node][to_node]
            total_time += duration_matrix[from_node][to_node]

            if from_node != 0:
                total_time += DEFAULT_SERVICE_TIME_MINUTES

            index = next_index

        if stops:
            if current_load > 0:
                order, current_load, extra_dist, extra_time = add_disposal_stop_if_needed(
                    request=request,
                    stops=stops,
                    ordered_points=ordered_points,
                    order=order,
                    current_load=current_load,
                    waste_type=last_waste_type,
                )
                total_dist += extra_dist
                total_time += extra_time

            ordered_points.append((request.depot.lat, request.depot.lng))

            route_coordinates = []
            try:
                raw_route = call_osrm_route(ordered_points)
                route_coordinates = [
                    RouteCoordinateDto(lat=p["lat"], lng=p["lng"])
                    for p in raw_route
                ]
            except Exception as e:
                print(
                    f"OSRM route geometry failed for truck {eligible_trucks[v].id}: {e}",
                    flush=True,
                )

            except Exception as e:
                print(
                    f"Route geometry failed for truck {eligible_trucks[v].id}: {e}. Falling back to OSRM.",
                    flush=True,
                )

                try:
                    raw_route = call_osrm_route(ordered_points)
                    route_coordinates = [
                        RouteCoordinateDto(lat=p["lat"], lng=p["lng"])
                        for p in raw_route
                    ]
                except Exception as osrm_error:
                    print(
                        f"OSRM fallback failed for truck {eligible_trucks[v].id}: {osrm_error}",
                        flush=True,
                    )

            mission = RoutingMissionDto(
                truckId=eligible_trucks[v].id,
                totalDistanceKm=round(total_dist / 1000, 2),
                totalDurationMinutes=round(total_time, 2),
                stops=stops,
                routeCoordinates=route_coordinates,
            )
            missions.append(mission)

            bin_stop_count = len([s for s in stops if s.stopType == "BIN_PICKUP"])
            disposal_stop_count = len([s for s in stops if s.stopType == "DISPOSAL_SITE"])

            duration = round(total_time, 2)
            distance_km = round(total_dist / 1000, 2)

            all_stops_counts.append(len(stops))
            all_durations.append(duration)
            all_distances.append(distance_km)

            print(
                f"[MISSION] truckId={eligible_trucks[v].id} | "
                f"binStops={bin_stop_count} | disposalStops={disposal_stop_count} | "
                f"duration={duration} min | distance={distance_km} km",
                flush=True,
            )

    all_bin_ids = {b.id for b in request.bins}
    dropped_bin_ids = sorted(all_bin_ids - served_bin_ids)

    print(f"Returned missions: {len(missions)}", flush=True)
    print(f"Served bins count: {len(served_bin_ids)}", flush=True)
    print(f"Dropped bins count: {len(dropped_bin_ids)}", flush=True)
    print(f"Dropped bin ids: {dropped_bin_ids}", flush=True)

    if all_stops_counts:
        avg_stops = sum(all_stops_counts) / len(all_stops_counts)
        avg_duration = sum(all_durations) / len(all_durations)
        avg_distance = sum(all_distances) / len(all_distances)

        print("\n========== BALANCING STATS ==========", flush=True)
        print(
            f"Stops -> min={min(all_stops_counts)}, max={max(all_stops_counts)}, avg={round(avg_stops, 2)}",
            flush=True,
        )
        print(
            f"Duration -> min={min(all_durations)}, max={max(all_durations)}, avg={round(avg_duration, 2)}",
            flush=True,
        )
        print(
            f"Distance -> min={min(all_distances)}, max={max(all_distances)}, avg={round(avg_distance, 2)}",
            flush=True,
        )
        print("=====================================\n", flush=True)

    return RoutingResponseDto(
        missions=missions,
        matrixSource=matrix_source,
        excludedTrucks=excluded_trucks,
        warningTrucks=warning_trucks,
        recommendedFuelStations=[],
        droppedBinIds=dropped_bin_ids,
    )


def solve_single_pass(
    request: RoutingRequestDto,
    eligible_trucks,
    excluded_trucks,
    warning_trucks,
) -> RoutingResponseDto:
    if not request.bins:
        print("No bins received in this pass. Returning empty response.", flush=True)
        return RoutingResponseDto(
            missions=[],
            matrixSource="NONE",
            excludedTrucks=excluded_trucks,
            warningTrucks=warning_trucks,
            recommendedFuelStations=[],
            droppedBinIds=[],
        )

    distance_matrix, duration_matrix, matrix_source = create_matrices(request)
    print(f"FINAL MATRIX SOURCE USED BY SOLVER = {matrix_source}", flush=True)

    demands = [0] + [max(1, int(round(b.estimatedLoadKg or 0))) for b in request.bins]
    capacities = [int(t.remainingCapacityKg or 0) for t in eligible_trucks]

    run_max_minutes = resolve_run_max_minutes(request.currentRun)

    print(f"Current run={request.currentRun}, run_max_minutes={run_max_minutes}", flush=True)
    print(f"Truck capacities={capacities}", flush=True)
    print(f"Bin demands={demands}", flush=True)
    print("Capacity dimension DISABLED: handled by disposal-site post-processing.", flush=True)
    print(f"Disposal sites count={len(request.disposalSites or [])}", flush=True)

    manager = pywrapcp.RoutingIndexManager(
        len(distance_matrix),
        len(eligible_trucks),
        0,
    )
    routing = pywrapcp.RoutingModel(manager)

    def duration_callback(from_i, to_i):
        from_node = manager.IndexToNode(from_i)
        to_node = manager.IndexToNode(to_i)

        travel_minutes = duration_matrix[from_node][to_node]
        service_minutes = DEFAULT_SERVICE_TIME_MINUTES if from_node != 0 else 0
        return travel_minutes + service_minutes

    def distance_callback(from_i, to_i):
        from_node = manager.IndexToNode(from_i)
        to_node = manager.IndexToNode(to_i)

        base_distance = distance_matrix[from_node][to_node]

        from_bin = request.bins[from_node - 1] if from_node != 0 else None
        to_bin = request.bins[to_node - 1] if to_node != 0 else None

        return base_distance + cluster_soft_penalty(from_bin, to_bin)

    def stop_callback(from_i):
        node = manager.IndexToNode(from_i)
        return 0 if node == 0 else 1

    duration_transit = routing.RegisterTransitCallback(duration_callback)
    distance_transit = routing.RegisterTransitCallback(distance_callback)
    stop_transit = routing.RegisterUnaryTransitCallback(stop_callback)

    routing.SetArcCostEvaluatorOfAllVehicles(distance_transit)

    routing.AddDimension(
        duration_transit,
        60,
        run_max_minutes,
        True,
        "Time",
    )

    max_possible_stops = len(request.bins)
    routing.AddDimension(
        stop_transit,
        0,
        max_possible_stops,
        True,
        "Stops",
    )

    time_dimension = routing.GetDimensionOrDie("Time")
    stops_dimension = routing.GetDimensionOrDie("Stops")

    time_dimension.SetGlobalSpanCostCoefficient(30)
    stops_dimension.SetGlobalSpanCostCoefficient(100)

    for vehicle_idx in range(len(eligible_trucks)):
        start_index = routing.Start(vehicle_idx)
        end_index = routing.End(vehicle_idx)

        time_dimension.CumulVar(start_index).SetRange(0, run_max_minutes)
        time_dimension.CumulVar(end_index).SetRange(0, run_max_minutes)

    target_stops_per_truck = math.ceil(len(request.bins) / len(eligible_trucks))
    soft_upper_bound = target_stops_per_truck + 1

    for v in range(len(eligible_trucks)):
        end_index = routing.End(v)

        stops_dimension.SetCumulVarSoftUpperBound(
            end_index,
            soft_upper_bound,
            30000,
        )

        routing.AddVariableMinimizedByFinalizer(time_dimension.CumulVar(end_index))
        routing.AddVariableMinimizedByFinalizer(stops_dimension.CumulVar(end_index))

    for node in range(1, len(distance_matrix)):
        bin_dto = request.bins[node - 1]
        penalty = compute_drop_penalty(bin_dto)
        category = normalize_category(bin_dto)

        compatible_vehicle_indices = get_compatible_vehicle_indices_for_bin(
            eligible_trucks, bin_dto
        )

        print(
            f"COMPAT DEBUG -> binId={bin_dto.id}, "
            f"zoneId={getattr(bin_dto, 'zoneId', None)}, "
            f"wasteType={getattr(bin_dto, 'wasteType', None)}, "
            f"compatibleVehicles={compatible_vehicle_indices}, "
            f"compatibleCount={len(compatible_vehicle_indices)}",
            flush=True,
        )

        node_index = manager.NodeToIndex(node)

        if compatible_vehicle_indices:
            allowed_vehicles = [int(v) for v in compatible_vehicle_indices]
            routing.VehicleVar(node_index).SetValues(allowed_vehicles)
        else:
            print(
                f"No compatible truck for binId={bin_dto.id}, "
                f"wasteType={getattr(bin_dto, 'wasteType', None)}, "
                f"zoneId={getattr(bin_dto, 'zoneId', None)}, "
                f"clusterId={getattr(bin_dto, 'clusterId', None)}. "
                f"Bin will be handled by category rules.",
                flush=True,
            )
        print(
            f"TIME WINDOWS DISABLED TEMPORARILY -> binId={bin_dto.id}",
            flush=True,
        )

       
        if category != "MANDATORY" and not is_bin_urgent(bin_dto):
              routing.AddDisjunction([node_index], penalty)

        print(
            f"Bin configured -> "
            f"binId={bin_dto.id}, "
            f"node={node}, "
            f"category={category}, "
            f"reason={bin_dto.decisionReason}, "
            f"wasteType={getattr(bin_dto, 'wasteType', None)}, "
            f"zoneId={getattr(bin_dto, 'zoneId', None)}, "
            f"clusterId={getattr(bin_dto, 'clusterId', None)}, "
            f"timeWindow=({getattr(bin_dto, 'windowStartMinutes', None)}, {getattr(bin_dto, 'windowEndMinutes', None)}), "
            f"compatibleVehicles={compatible_vehicle_indices}, "
            f"priority={bin_dto.predictedPriority}, "
            f"predictedHoursToFull={bin_dto.predictedHoursToFull}, "
            f"feedbackScore={bin_dto.feedbackScore}, "
            f"postponementCount={bin_dto.postponementCount}, "
            f"opportunisticScore={bin_dto.opportunisticScore}, "
            f"mandatory={bin_dto.mandatory}, "
            f"dropPenalty={penalty}, "
            f"droppable={category != 'MANDATORY'}",
            flush=True,
        )

    params = pywrapcp.DefaultRoutingSearchParameters()
    params.first_solution_strategy = routing_enums_pb2.FirstSolutionStrategy.PATH_CHEAPEST_ARC
    params.local_search_metaheuristic = routing_enums_pb2.LocalSearchMetaheuristic.GUIDED_LOCAL_SEARCH
    params.time_limit.seconds = 20

    print("Solving OR-Tools routing problem...", flush=True)
    solution = routing.SolveWithParameters(params)

    if not solution:
        print("No OR-Tools solution found for this pass.", flush=True)
        return RoutingResponseDto(
            missions=[],
            matrixSource=matrix_source,
            excludedTrucks=excluded_trucks,
            warningTrucks=warning_trucks,
            recommendedFuelStations=[],
            droppedBinIds=[b.id for b in request.bins],
        )

    return build_response_from_solution(
        request=request,
        eligible_trucks=eligible_trucks,
        excluded_trucks=excluded_trucks,
        warning_trucks=warning_trucks,
        distance_matrix=distance_matrix,
        duration_matrix=duration_matrix,
        matrix_source=matrix_source,
        solution=solution,
        routing=routing,
        manager=manager,
    )


def count_served_mandatory(response: RoutingResponseDto, mandatory_bin_ids: set[int]) -> int:
    served = 0
    dropped = set(response.droppedBinIds or [])

    for bin_id in mandatory_bin_ids:
        if bin_id not in dropped:
            served += 1

    return served


def count_served_total(response: RoutingResponseDto) -> int:
    served = 0
    for mission in response.missions or []:
        for stop in mission.stops or []:
            if stop.stopType == "BIN_PICKUP":
                served += 1
    return served


def choose_best_response(
    pass1_response: RoutingResponseDto | None,
    pass2_response: RoutingResponseDto | None,
    mandatory_bin_ids: set[int],
) -> RoutingResponseDto:
    if pass1_response is None and pass2_response is None:
        return RoutingResponseDto(
            missions=[],
            matrixSource="NONE",
            excludedTrucks=[],
            warningTrucks=[],
            recommendedFuelStations=[],
            droppedBinIds=[],
        )

    if pass1_response is None:
        return pass2_response

    if pass2_response is None:
        return pass1_response

    pass1_mandatory_served = count_served_mandatory(pass1_response, mandatory_bin_ids)
    pass2_mandatory_served = count_served_mandatory(pass2_response, mandatory_bin_ids)

    print(
        f"Pass comparison => "
        f"pass1_mandatory_served={pass1_mandatory_served}, "
        f"pass2_mandatory_served={pass2_mandatory_served}",
        flush=True,
    )

    if pass2_mandatory_served < pass1_mandatory_served:
        print("Choosing PASS 1 because PASS 2 served fewer mandatory bins.", flush=True)
        return pass1_response

    pass1_total_served = count_served_total(pass1_response)
    pass2_total_served = count_served_total(pass2_response)

    print(
        f"Pass comparison => "
        f"pass1_total_served={pass1_total_served}, "
        f"pass2_total_served={pass2_total_served}",
        flush=True,
    )

    if pass2_total_served > pass1_total_served:
        print(
            "Choosing PASS 2 because it served more bins without hurting mandatory coverage.",
            flush=True,
        )
        return pass2_response

    print("Choosing PASS 1 by default.", flush=True)
    return pass1_response


def optimize_routing_core(request: RoutingRequestDto) -> RoutingResponseDto:
    print(
        f"Optimizing with {len(request.bins)} bins and {len(request.trucks)} trucks",
        flush=True,
    )
    print(f"Active incidents received: {len(request.activeIncidents)}", flush=True)
    print(f"Disposal sites received: {len(request.disposalSites or [])}", flush=True)
    print(f"Current run received: {request.currentRun}", flush=True)

    eligible_trucks, excluded_trucks, warning_trucks = filter_eligible_trucks(
        request.trucks,
        request.activeIncidents,
    )

    print(f"Eligible trucks after status/incident filtering: {len(eligible_trucks)}", flush=True)
    print(f"Excluded trucks count: {len(excluded_trucks)}", flush=True)
    print(f"Warning trucks count: {len(warning_trucks)}", flush=True)

    if not eligible_trucks or not request.bins:
        print("No eligible trucks or no bins received. Returning empty missions.", flush=True)
        return RoutingResponseDto(
            missions=[],
            matrixSource="NONE",
            excludedTrucks=excluded_trucks,
            warningTrucks=warning_trucks,
            recommendedFuelStations=[],
            droppedBinIds=[],
        )

    zone_counter = Counter([getattr(t, "zoneId", None) for t in eligible_trucks])
    print(f"Eligible truck zones: {dict(zone_counter)}", flush=True)

    mandatory_bins, opportunistic_bins, reportable_bins = split_bins_by_category(request.bins)
    opportunistic_bins = sort_opportunistic_bins(opportunistic_bins)
    opportunistic_bins = filter_opportunistic_by_distance(
        request=request,
        mandatory_bins=mandatory_bins,
        opportunistic_bins=opportunistic_bins,
    )

    print(
        f"Split bins => mandatory={len(mandatory_bins)}, "
        f"opportunistic={len(opportunistic_bins)}, "
        f"reportable={len(reportable_bins)}",
        flush=True,
    )

    if opportunistic_bins:
        print("Sorted opportunistic bins by opportunisticScore:", flush=True)
        for b in opportunistic_bins:
            print(
                f"binId={b.id}, opportunisticScore={b.opportunisticScore}, "
                f"feedbackScore={b.feedbackScore}, predictedHoursToFull={b.predictedHoursToFull}, "
                f"zoneId={b.zoneId}, clusterId={b.clusterId}",
                flush=True,
            )

    pass1_response = None
    pass2_response = None

    mandatory_bin_ids = {b.id for b in mandatory_bins}

    if mandatory_bins:
        print("Starting PASS 1: mandatory bins only", flush=True)
        pass1_request = build_sub_request(request, mandatory_bins)
        pass1_response = solve_single_pass(
            pass1_request,
            eligible_trucks,
            excluded_trucks,
            warning_trucks,
        )
    else:
        print("No mandatory bins found. Skipping PASS 1.", flush=True)

    pass2_bins = mandatory_bins + opportunistic_bins

    if len(pass2_bins) > MAX_PASS2_BINS:
        print(
            f"Skipping PASS 2 because it has too many bins: {len(pass2_bins)} > {MAX_PASS2_BINS}. "
            f"Using PASS 1 result only.",
            flush=True,
        )
        pass2_response = None
    elif pass2_bins:
        print("Starting PASS 2: mandatory + opportunistic bins", flush=True)
        pass2_request = build_sub_request(request, pass2_bins)
        pass2_response = solve_single_pass(
            pass2_request,
            eligible_trucks,
            excluded_trucks,
            warning_trucks,
        )
    else:
        print("No bins available for PASS 2.", flush=True)

    if pass1_response is None and pass2_response is None and reportable_bins:
        print("Fallback solve: reportable bins only", flush=True)
        fallback_request = build_sub_request(request, reportable_bins)
        return solve_single_pass(
            fallback_request,
            eligible_trucks,
            excluded_trucks,
            warning_trucks,
        )

    best_response = choose_best_response(
        pass1_response=pass1_response,
        pass2_response=pass2_response,
        mandatory_bin_ids=mandatory_bin_ids,
    )

    print(
        f"Final selected response => missions={len(best_response.missions)}, "
        f"served={count_served_total(best_response)}, "
        f"dropped={len(best_response.droppedBinIds)}",
        flush=True,
    )
   

    return best_response





def optimize_routing(request: RoutingRequestDto) -> RoutingResponseDto:
    grouped_bins = split_bins_by_waste_family(request.bins)

    print(
        "Waste family split => "
        + ", ".join([f"{family}={len(bins)}" for family, bins in grouped_bins.items()]),
        flush=True,
    )

    if len(grouped_bins) <= 1:
        return optimize_routing_core(request)

    responses = []
    all_input_bin_ids = [b.id for b in request.bins or []]

    family_order = ["ORGANIC", "YELLOW", "WHITE", "UNKNOWN"]

    for family in family_order:
        family_bins = grouped_bins.get(family, [])

        if not family_bins:
            continue

        print(
            f"Starting optimization for waste family={family} | bins={len(family_bins)}",
            flush=True,
        )

        family_request = build_family_request(request, family_bins)
        family_response = optimize_routing_core(family_request)
        responses.append(family_response)

    merged = merge_family_responses(responses, all_input_bin_ids)

    print(
        f"Waste family merged response => missions={len(merged.missions)}, "
        f"dropped={len(merged.droppedBinIds)}",
        flush=True,
    )

    return merged
