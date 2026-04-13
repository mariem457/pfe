import math
from ortools.constraint_solver import pywrapcp, routing_enums_pb2

from config.settings import DEFAULT_MAX_ROUTE_MINUTES
from models.routing_models import (
    RoutingRequestDto,
    RoutingResponseDto,
    RoutingMissionDto,
    RoutingStopDto,
    RouteCoordinateDto
)
from services.incident_service import filter_eligible_trucks
from services.matrix_service import create_matrices
from services.osrm_service import call_osrm_route


DEFAULT_BIN_DROPOUT_PENALTY = 10000
MANDATORY_BASE_PENALTY = 100000
OPPORTUNISTIC_BASE_PENALTY = 30000
REPORTABLE_BASE_PENALTY = 8000

MORNING_RUN_MAX_MINUTES = 6 * 60
EVENING_RUN_MAX_MINUTES = 6 * 60

# depot window: full day
DEFAULT_DEPOT_WINDOW_START = 0
DEFAULT_DEPOT_WINDOW_END = 24 * 60


def normalize_text(value) -> str:
    if value is None:
        return ""
    return str(value).strip().upper()


def is_truck_compatible_with_bin(truck, bin_dto) -> bool:
    bin_waste_type = normalize_text(getattr(bin_dto, "wasteType", None))
    if not bin_waste_type:
        return True

    supported_types = getattr(truck, "supportedWasteTypes", None)
    if not supported_types:
        return True

    normalized_supported = {normalize_text(w) for w in supported_types if w is not None}
    return bin_waste_type in normalized_supported


def get_compatible_vehicle_indices_for_bin(eligible_trucks, bin_dto) -> list[int]:
    compatible_vehicle_indices = []

    for vehicle_index, truck in enumerate(eligible_trucks):
        if is_truck_compatible_with_bin(truck, bin_dto):
            compatible_vehicle_indices.append(vehicle_index)

    return compatible_vehicle_indices


def normalize_category(bin_dto) -> str:
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


def compute_drop_penalty(bin_dto) -> int:
    priority = float(bin_dto.predictedPriority or 0.0)
    predicted_hours = bin_dto.predictedHoursToFull
    feedback_score = float(bin_dto.feedbackScore or 0.0)
    postponement_count = int(bin_dto.postponementCount or 0)
    opportunistic_score = float(bin_dto.opportunisticScore or 0.0)
    category = normalize_category(bin_dto)

    if category == "MANDATORY":
        penalty = MANDATORY_BASE_PENALTY
        penalty += int(priority * 10000)
        penalty += int(feedback_score * 3000)
        penalty += postponement_count * 1500

        if predicted_hours is not None:
            if predicted_hours <= 6:
                penalty += 30000
            elif predicted_hours <= 12:
                penalty += 15000

        return penalty

    if category == "OPPORTUNISTIC":
        penalty = OPPORTUNISTIC_BASE_PENALTY
        penalty += int(priority * 12000)
        penalty += int(feedback_score * 1500)
        penalty += postponement_count * 800
        penalty += int(opportunistic_score * 1000)

        if predicted_hours is not None:
            if predicted_hours <= 12:
                penalty += 10000
            elif predicted_hours <= 24:
                penalty += 5000

        return penalty

    if category == "REPORTABLE":
        penalty = REPORTABLE_BASE_PENALTY
        penalty += int(priority * 5000)
        penalty += int(feedback_score * 500)
        penalty += postponement_count * 300
        return penalty

    mandatory = bool(bin_dto.mandatory) if bin_dto.mandatory is not None else False

    if mandatory:
        return 100000

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
            -float(b.predictedHoursToFull or 999999.0)
        ),
        reverse=True
    )


def build_sub_request(original_request: RoutingRequestDto, selected_bins) -> RoutingRequestDto:
    return RoutingRequestDto(
        depot=original_request.depot,
        trafficMode=original_request.trafficMode,
        currentRun=original_request.currentRun,
        bins=selected_bins,
        trucks=original_request.trucks,
        activeIncidents=original_request.activeIncidents
    )


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
    manager
) -> RoutingResponseDto:
    missions = []
    served_bin_ids = set()

    for v in range(len(eligible_trucks)):
        index = routing.Start(v)
        stops = []
        total_dist = 0
        total_time = 0
        order = 1

        ordered_points = [(request.depot.lat, request.depot.lng)]

        while not routing.IsEnd(index):
            next_index = solution.Value(routing.NextVar(index))
            from_node = manager.IndexToNode(index)
            to_node = manager.IndexToNode(next_index)

            if to_node != 0:
                selected_bin = request.bins[to_node - 1]
                served_bin_ids.add(selected_bin.id)

                stops.append(
                    RoutingStopDto(
                        binId=selected_bin.id,
                        orderIndex=order
                    )
                )
                order += 1
                ordered_points.append((selected_bin.lat, selected_bin.lng))

            total_dist += distance_matrix[from_node][to_node]
            total_time += duration_matrix[from_node][to_node]
            index = next_index

        if stops:
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
                    flush=True
                )

            mission = RoutingMissionDto(
                truckId=eligible_trucks[v].id,
                totalDistanceKm=round(total_dist / 1000, 2),
                totalDurationMinutes=round(total_time, 2),
                stops=stops,
                routeCoordinates=route_coordinates
            )
            missions.append(mission)

    all_bin_ids = {b.id for b in request.bins}
    dropped_bin_ids = sorted(all_bin_ids - served_bin_ids)

    print(f"Returned missions: {len(missions)}", flush=True)
    print(f"Served bins count: {len(served_bin_ids)}", flush=True)
    print(f"Dropped bins count: {len(dropped_bin_ids)}", flush=True)
    print(f"Dropped bin ids: {dropped_bin_ids}", flush=True)

    return RoutingResponseDto(
        missions=missions,
        matrixSource=matrix_source,
        excludedTrucks=excluded_trucks,
        warningTrucks=warning_trucks,
        recommendedFuelStations=[],
        droppedBinIds=dropped_bin_ids
    )


def solve_single_pass(
    request: RoutingRequestDto,
    eligible_trucks,
    excluded_trucks,
    warning_trucks
) -> RoutingResponseDto:
    if not request.bins:
        print("No bins received in this pass. Returning empty response.", flush=True)
        return RoutingResponseDto(
            missions=[],
            matrixSource="NONE",
            excludedTrucks=excluded_trucks,
            warningTrucks=warning_trucks,
            recommendedFuelStations=[],
            droppedBinIds=[]
        )

    distance_matrix, duration_matrix, matrix_source = create_matrices(request)

    demands = [0] + [max(1, int(round(b.estimatedLoadKg or 0))) for b in request.bins]
    capacities = [int(t.remainingCapacityKg) for t in eligible_trucks]

    run_max_minutes = resolve_run_max_minutes(request.currentRun)
    max_route_minutes = [run_max_minutes for _ in eligible_trucks]

    print(
        f"Current run={request.currentRun}, run_max_minutes={run_max_minutes}",
        flush=True
    )

    manager = pywrapcp.RoutingIndexManager(
        len(distance_matrix),
        len(eligible_trucks),
        0
    )

    routing = pywrapcp.RoutingModel(manager)

    def distance_callback(from_i, to_i):
        from_node = manager.IndexToNode(from_i)
        to_node = manager.IndexToNode(to_i)
        return distance_matrix[from_node][to_node]

    def duration_callback(from_i, to_i):
        from_node = manager.IndexToNode(from_i)
        to_node = manager.IndexToNode(to_i)
        return duration_matrix[from_node][to_node]

    def demand_callback(from_i):
        return demands[manager.IndexToNode(from_i)]

    def stop_callback(from_i):
        node = manager.IndexToNode(from_i)
        return 0 if node == 0 else 1

    distance_transit = routing.RegisterTransitCallback(distance_callback)
    duration_transit = routing.RegisterTransitCallback(duration_callback)
    demand = routing.RegisterUnaryTransitCallback(demand_callback)
    stop_transit = routing.RegisterUnaryTransitCallback(stop_callback)

    # objective
    routing.SetArcCostEvaluatorOfAllVehicles(duration_transit)

    routing.AddDimensionWithVehicleCapacity(
        demand,
        0,
        capacities,
        True,
        "Capacity"
    )

    # Time dimension = travel time in minutes
    routing.AddDimensionWithVehicleCapacity(
        duration_transit,
        60,  # slack: allows waiting if truck arrives before opening window
        max_route_minutes,
        True,
        "Time"
    )

    max_possible_stops = len(request.bins)
    routing.AddDimension(
        stop_transit,
        0,
        max_possible_stops,
        True,
        "Stops"
    )

    time_dimension = routing.GetDimensionOrDie("Time")
    stops_dimension = routing.GetDimensionOrDie("Stops")

    time_dimension.SetGlobalSpanCostCoefficient(100)
    stops_dimension.SetGlobalSpanCostCoefficient(1000)

    # depot windows
    for vehicle_idx in range(len(eligible_trucks)):
        start_index = routing.Start(vehicle_idx)
        end_index = routing.End(vehicle_idx)

        time_dimension.CumulVar(start_index).SetRange(
            DEFAULT_DEPOT_WINDOW_START,
            DEFAULT_DEPOT_WINDOW_END
        )
        time_dimension.CumulVar(end_index).SetRange(
            DEFAULT_DEPOT_WINDOW_START,
            DEFAULT_DEPOT_WINDOW_END
        )

    target_stops_per_truck = math.ceil(len(request.bins) / len(eligible_trucks))
    soft_upper_bound = target_stops_per_truck + 1

    for v in range(len(eligible_trucks)):
        end_index = routing.End(v)

        stops_dimension.SetCumulVarSoftUpperBound(
            end_index,
            soft_upper_bound,
            10000
        )

        routing.AddVariableMinimizedByFinalizer(time_dimension.CumulVar(end_index))
        routing.AddVariableMinimizedByFinalizer(stops_dimension.CumulVar(end_index))

    # compatibility + time windows
    for node in range(1, len(distance_matrix)):
        bin_dto = request.bins[node - 1]
        penalty = compute_drop_penalty(bin_dto)
        category = normalize_category(bin_dto)

        compatible_vehicle_indices = get_compatible_vehicle_indices_for_bin(eligible_trucks, bin_dto)
        node_index = manager.NodeToIndex(node)

        if compatible_vehicle_indices:
            routing.SetAllowedVehiclesForIndex(compatible_vehicle_indices, node_index)
        else:
            print(
                f"No compatible truck for binId={bin_dto.id}, wasteType={getattr(bin_dto, 'wasteType', None)}. "
                f"Bin will be droppable only via penalty.",
                flush=True
            )

        # Apply time window only if both values exist
        if bin_dto.windowStartMinutes is not None and bin_dto.windowEndMinutes is not None:
            start_min = int(bin_dto.windowStartMinutes)
            end_min = int(bin_dto.windowEndMinutes)

            if start_min <= end_min:
                time_dimension.CumulVar(node_index).SetRange(start_min, end_min)
            else:
                print(
                    f"Invalid time window for binId={bin_dto.id}: "
                    f"start={start_min}, end={end_min}",
                    flush=True
                )

        routing.AddDisjunction([node_index], penalty)

        print(
            f"Optional bin configured -> "
            f"binId={bin_dto.id}, "
            f"node={node}, "
            f"category={category}, "
            f"reason={bin_dto.decisionReason}, "
            f"wasteType={getattr(bin_dto, 'wasteType', None)}, "
            f"timeWindow=({getattr(bin_dto, 'windowStartMinutes', None)}, {getattr(bin_dto, 'windowEndMinutes', None)}), "
            f"compatibleVehicles={compatible_vehicle_indices}, "
            f"priority={bin_dto.predictedPriority}, "
            f"predictedHoursToFull={bin_dto.predictedHoursToFull}, "
            f"feedbackScore={bin_dto.feedbackScore}, "
            f"postponementCount={bin_dto.postponementCount}, "
            f"opportunisticScore={bin_dto.opportunisticScore}, "
            f"mandatory={bin_dto.mandatory}, "
            f"dropPenalty={penalty}",
            flush=True
        )

    params = pywrapcp.DefaultRoutingSearchParameters()
    params.first_solution_strategy = routing_enums_pb2.FirstSolutionStrategy.PATH_CHEAPEST_ARC
    params.local_search_metaheuristic = routing_enums_pb2.LocalSearchMetaheuristic.GUIDED_LOCAL_SEARCH
    params.time_limit.seconds = 10

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
            droppedBinIds=[b.id for b in request.bins]
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
        manager=manager
    )


def count_served_mandatory(response: RoutingResponseDto, mandatory_bin_ids: set[int]) -> int:
    served = 0
    dropped = set(response.droppedBinIds or [])

    for bin_id in mandatory_bin_ids:
        if bin_id not in dropped:
            served += 1

    return served


def count_served_total(response: RoutingResponseDto) -> int:
    total_dropped = len(response.droppedBinIds or [])
    total_bins = total_dropped

    for mission in response.missions or []:
        total_bins += len(mission.stops or [])

    return total_bins


def choose_best_response(
    pass1_response: RoutingResponseDto | None,
    pass2_response: RoutingResponseDto | None,
    mandatory_bin_ids: set[int]
) -> RoutingResponseDto:
    if pass1_response is None and pass2_response is None:
        return RoutingResponseDto(
            missions=[],
            matrixSource="NONE",
            excludedTrucks=[],
            warningTrucks=[],
            recommendedFuelStations=[],
            droppedBinIds=[]
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
        flush=True
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
        flush=True
    )

    if pass2_total_served > pass1_total_served:
        print("Choosing PASS 2 because it served more bins without hurting mandatory coverage.", flush=True)
        return pass2_response

    print("Choosing PASS 1 by default.", flush=True)
    return pass1_response


def optimize_routing(request: RoutingRequestDto) -> RoutingResponseDto:
    print(
        f"Optimizing with {len(request.bins)} bins and {len(request.trucks)} trucks",
        flush=True
    )
    print(
        f"Active incidents received: {len(request.activeIncidents)}",
        flush=True
    )
    print(
        f"Current run received: {request.currentRun}",
        flush=True
    )

    eligible_trucks, excluded_trucks, warning_trucks = filter_eligible_trucks(
        request.trucks,
        request.activeIncidents
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
            droppedBinIds=[]
        )

    mandatory_bins, opportunistic_bins, reportable_bins = split_bins_by_category(request.bins)
    opportunistic_bins = sort_opportunistic_bins(opportunistic_bins)

    print(
        f"Split bins => mandatory={len(mandatory_bins)}, "
        f"opportunistic={len(opportunistic_bins)}, "
        f"reportable={len(reportable_bins)}",
        flush=True
    )

    if opportunistic_bins:
        print("Sorted opportunistic bins by opportunisticScore:", flush=True)
        for b in opportunistic_bins:
            print(
                f"  binId={b.id}, opportunisticScore={b.opportunisticScore}, "
                f"feedbackScore={b.feedbackScore}, predictedHoursToFull={b.predictedHoursToFull}",
                flush=True
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
            warning_trucks
        )
    else:
        print("No mandatory bins found. Skipping PASS 1.", flush=True)

    pass2_bins = mandatory_bins + opportunistic_bins

    if pass2_bins:
        print("Starting PASS 2: mandatory + opportunistic bins", flush=True)
        pass2_request = build_sub_request(request, pass2_bins)
        pass2_response = solve_single_pass(
            pass2_request,
            eligible_trucks,
            excluded_trucks,
            warning_trucks
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
            warning_trucks
        )

    best_response = choose_best_response(
        pass1_response=pass1_response,
        pass2_response=pass2_response,
        mandatory_bin_ids=mandatory_bin_ids
    )

    print(
        f"Final selected response => missions={len(best_response.missions)}, "
        f"dropped={len(best_response.droppedBinIds)}",
        flush=True
    )

    return best_response