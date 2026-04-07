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


def normalize_category(bin_dto) -> str:
    category = (bin_dto.decisionCategory or "").upper()

    if category in {"MANDATORY", "OPPORTUNISTIC", "REPORTABLE"}:
        return category

    if bool(bin_dto.mandatory):
        return "MANDATORY"

    if bool(bin_dto.opportunistic):
        return "OPPORTUNISTIC"

    if bool(bin_dto.reportable):
        return "REPORTABLE"

    return "UNKNOWN"


def compute_drop_penalty(bin_dto) -> int:
    priority = float(bin_dto.predictedPriority or 0.0)
    predicted_hours = bin_dto.predictedHoursToFull
    feedback_score = float(bin_dto.feedbackScore or 0.0)
    postponement_count = int(bin_dto.postponementCount or 0)
    category = normalize_category(bin_dto)

    # 1) Mandatory bins: extremely expensive to drop
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

    # 2) Opportunistic bins: medium penalty
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

    # 3) Reportable bins: lower penalty
    if category == "REPORTABLE":
        penalty = REPORTABLE_BASE_PENALTY
        penalty += int(priority * 5000)
        penalty += int(feedback_score * 500)
        penalty += postponement_count * 300
        return penalty

    # 4) Fallback old behavior
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


def optimize_routing(request: RoutingRequestDto) -> RoutingResponseDto:
    print(
        f"Optimizing with {len(request.bins)} bins and {len(request.trucks)} trucks",
        flush=True
    )
    print(
        f"Active incidents received: {len(request.activeIncidents)}",
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

    distance_matrix, duration_matrix, matrix_source = create_matrices(request)

    demands = [0] + [max(1, int(round(b.estimatedLoadKg or 0))) for b in request.bins]
    capacities = [int(t.remainingCapacityKg) for t in eligible_trucks]
    max_route_minutes = [DEFAULT_MAX_ROUTE_MINUTES for _ in eligible_trucks]

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

    duration_transit = routing.RegisterTransitCallback(duration_callback)
    demand = routing.RegisterUnaryTransitCallback(demand_callback)
    stop_transit = routing.RegisterUnaryTransitCallback(stop_callback)

    routing.SetArcCostEvaluatorOfAllVehicles(duration_transit)

    routing.AddDimensionWithVehicleCapacity(
        demand,
        0,
        capacities,
        True,
        "Capacity"
    )

    routing.AddDimensionWithVehicleCapacity(
        duration_transit,
        0,
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

    for node in range(1, len(distance_matrix)):
        bin_dto = request.bins[node - 1]
        penalty = compute_drop_penalty(bin_dto)
        category = normalize_category(bin_dto)

        routing.AddDisjunction([manager.NodeToIndex(node)], penalty)

        print(
            f"Optional bin configured -> "
            f"binId={bin_dto.id}, "
            f"node={node}, "
            f"category={category}, "
            f"reason={bin_dto.decisionReason}, "
            f"priority={bin_dto.predictedPriority}, "
            f"predictedHoursToFull={bin_dto.predictedHoursToFull}, "
            f"feedbackScore={bin_dto.feedbackScore}, "
            f"postponementCount={bin_dto.postponementCount}, "
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
        print("No OR-Tools solution found.", flush=True)
        return RoutingResponseDto(
            missions=[],
            matrixSource=matrix_source,
            excludedTrucks=excluded_trucks,
            warningTrucks=warning_trucks,
            recommendedFuelStations=[],
            droppedBinIds=[b.id for b in request.bins]
        )

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