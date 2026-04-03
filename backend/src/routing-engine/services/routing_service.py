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

from services.fuel_service import compute_max_distance_from_fuel
from services.incident_service import filter_eligible_trucks
from services.matrix_service import create_matrices
from services.osrm_service import call_osrm_route


def optimize_routing(request: RoutingRequestDto) -> RoutingResponseDto:
    print(f"Optimizing with {len(request.bins)} bins and {len(request.trucks)} trucks", flush=True)
    print(f"Active incidents received: {len(request.activeIncidents)}", flush=True)

    eligible_trucks, excluded_trucks, warning_trucks = filter_eligible_trucks(
        request.trucks,
        request.activeIncidents
    )

    if not eligible_trucks or not request.bins:
        return RoutingResponseDto(
            missions=[],
            matrixSource="NONE",
            excludedTrucks=excluded_trucks,
            warningTrucks=warning_trucks,
            recommendedFuelStations=[]
        )

    distance_matrix, duration_matrix, matrix_source = create_matrices(request)

    demands = [0] + [max(1, int(round(b.estimatedLoadKg or 0))) for b in request.bins]
    capacities = [int(t.remainingCapacityKg) for t in eligible_trucks]
    max_distances = [compute_max_distance_from_fuel(t) for t in eligible_trucks]
    max_route_minutes = [DEFAULT_MAX_ROUTE_MINUTES for _ in eligible_trucks]

    manager = pywrapcp.RoutingIndexManager(
        len(distance_matrix),
        len(eligible_trucks),
        0
    )

    routing = pywrapcp.RoutingModel(manager)

    def distance_callback(from_i, to_i):
        return distance_matrix[manager.IndexToNode(from_i)][manager.IndexToNode(to_i)]

    def duration_callback(from_i, to_i):
        return duration_matrix[manager.IndexToNode(from_i)][manager.IndexToNode(to_i)]

    def demand_callback(from_i):
        return demands[manager.IndexToNode(from_i)]

    def stop_callback(from_i):
        node = manager.IndexToNode(from_i)
        return 0 if node == 0 else 1

    distance_transit = routing.RegisterTransitCallback(distance_callback)
    duration_transit = routing.RegisterTransitCallback(duration_callback)
    demand = routing.RegisterUnaryTransitCallback(demand_callback)
    stop_transit = routing.RegisterUnaryTransitCallback(stop_callback)

    routing.SetArcCostEvaluatorOfAllVehicles(duration_transit)

    routing.AddDimensionWithVehicleCapacity(demand, 0, capacities, True, "Capacity")
    routing.AddDimensionWithVehicleCapacity(distance_transit, 0, max_distances, True, "Distance")
    routing.AddDimensionWithVehicleCapacity(duration_transit, 0, max_route_minutes, True, "Time")

    routing.AddDimension(stop_transit, 0, len(request.bins), True, "Stops")

    time_dimension = routing.GetDimensionOrDie("Time")
    stops_dimension = routing.GetDimensionOrDie("Stops")

    time_dimension.SetGlobalSpanCostCoefficient(100)
    stops_dimension.SetGlobalSpanCostCoefficient(1000)

    target_stops_per_truck = math.ceil(len(request.bins) / len(eligible_trucks))

    for v in range(len(eligible_trucks)):
        end_index = routing.End(v)

        stops_dimension.SetCumulVarSoftUpperBound(end_index, target_stops_per_truck + 1, 10000)

        routing.AddVariableMinimizedByFinalizer(time_dimension.CumulVar(end_index))
        routing.AddVariableMinimizedByFinalizer(stops_dimension.CumulVar(end_index))

    params = pywrapcp.DefaultRoutingSearchParameters()
    params.first_solution_strategy = routing_enums_pb2.FirstSolutionStrategy.PATH_CHEAPEST_ARC
    params.local_search_metaheuristic = routing_enums_pb2.LocalSearchMetaheuristic.GUIDED_LOCAL_SEARCH
    params.time_limit.seconds = 10

    solution = routing.SolveWithParameters(params)

    if not solution:
        return RoutingResponseDto(
            missions=[],
            matrixSource=matrix_source,
            excludedTrucks=excluded_trucks,
            warningTrucks=warning_trucks,
            recommendedFuelStations=[]
        )

    missions = []

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
                b = request.bins[to_node - 1]
                stops.append(RoutingStopDto(binId=b.id, orderIndex=order))
                order += 1
                ordered_points.append((b.lat, b.lng))

            total_dist += distance_matrix[from_node][to_node]
            total_time += duration_matrix[from_node][to_node]

            index = next_index

        if stops:
            ordered_points.append((request.depot.lat, request.depot.lng))

            route_coordinates = []
            try:
                raw = call_osrm_route(ordered_points)
                route_coordinates = [RouteCoordinateDto(lat=p["lat"], lng=p["lng"]) for p in raw]
            except Exception as e:
                print(f"OSRM failed: {e}", flush=True)

            missions.append(
                RoutingMissionDto(
                    truckId=eligible_trucks[v].id,
                    totalDistanceKm=round(total_dist / 1000, 2),
                    totalDurationMinutes=round(total_time, 2),
                    stops=stops,
                    routeCoordinates=route_coordinates
                )
            )

    return RoutingResponseDto(
        missions=missions,
        matrixSource=matrix_source,
        excludedTrucks=excluded_trucks,
        warningTrucks=warning_trucks,
        recommendedFuelStations=[]
    )