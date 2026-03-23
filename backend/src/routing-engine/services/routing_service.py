from ortools.constraint_solver import pywrapcp, routing_enums_pb2
from services.matrix_service import create_matrices
from services.fuel_service import compute_max_distance_from_fuel
from services.osrm_service import call_osrm_route
from models.routing_models import *


def optimize_routing(request: RoutingRequestDto) -> RoutingResponseDto:
    print(
        f"Optimizing with {len(request.bins)} bins and {len(request.trucks)} trucks",
        flush=True
    )

    if not request.trucks or not request.bins:
        print("No trucks or no bins received. Returning empty missions.", flush=True)
        return RoutingResponseDto(missions=[])

    distance_matrix, duration_matrix = create_matrices(request)

    if len(distance_matrix) > 1 and len(distance_matrix[0]) > 1:
        print(
            f"Distance depot -> first point (m): {distance_matrix[0][1]}",
            flush=True
        )
        print(
            f"Duration depot -> first point (min): {duration_matrix[0][1]}",
            flush=True
        )

    demands = [0] + [int(b.estimatedLoadKg) for b in request.bins]
    capacities = [int(t.remainingCapacityKg) for t in request.trucks]
    max_distances = [compute_max_distance_from_fuel(t) for t in request.trucks]

    print(f"Demands: {demands}", flush=True)
    print(f"Vehicle capacities: {capacities}", flush=True)
    print(f"Max distances from fuel (m): {max_distances}", flush=True)

    manager = pywrapcp.RoutingIndexManager(
        len(distance_matrix),
        len(request.trucks),
        0
    )

    routing = pywrapcp.RoutingModel(manager)

    def distance_callback(from_i, to_i):
        return distance_matrix[manager.IndexToNode(from_i)][manager.IndexToNode(to_i)]

    transit = routing.RegisterTransitCallback(distance_callback)
    routing.SetArcCostEvaluatorOfAllVehicles(transit)

    def demand_callback(from_i):
        return demands[manager.IndexToNode(from_i)]

    demand = routing.RegisterUnaryTransitCallback(demand_callback)

    routing.AddDimensionWithVehicleCapacity(
        demand, 0, capacities, True, "Capacity"
    )

    routing.AddDimensionWithVehicleCapacity(
        transit, 0, max_distances, True, "Distance"
    )

    params = pywrapcp.DefaultRoutingSearchParameters()
    params.first_solution_strategy = routing_enums_pb2.FirstSolutionStrategy.PATH_CHEAPEST_ARC
    params.local_search_metaheuristic = routing_enums_pb2.LocalSearchMetaheuristic.GUIDED_LOCAL_SEARCH
    params.time_limit.seconds = 8

    print("Solving OR-Tools routing problem...", flush=True)
    solution = routing.SolveWithParameters(params)

    if not solution:
        print("No OR-Tools solution found.", flush=True)
        return RoutingResponseDto(missions=[])

    missions = []

    for v in range(len(request.trucks)):
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

                stops.append(RoutingStopDto(
                    binId=selected_bin.id,
                    orderIndex=order
                ))
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
                print(
                    f"Route geometry loaded for truck {request.trucks[v].id}: {len(route_coordinates)} points",
                    flush=True
                )
            except Exception as e:
                print(
                    f"OSRM route geometry failed for truck {request.trucks[v].id}: {e}",
                    flush=True
                )

            mission = RoutingMissionDto(
                truckId=request.trucks[v].id,
                totalDistanceKm=round(total_dist / 1000, 2),
                totalDurationMinutes=round(total_time, 2),
                stops=stops,
                routeCoordinates=route_coordinates
            )
            missions.append(mission)

            print(
                f"Mission created for truck {request.trucks[v].id}: "
                f"distance={mission.totalDistanceKm} km, "
                f"duration={mission.totalDurationMinutes} min, "
                f"stops={len(mission.stops)}, "
                f"geometryPoints={len(mission.routeCoordinates)}",
                flush=True
            )

    print(f"Returned missions: {len(missions)}", flush=True)
    return RoutingResponseDto(missions=missions)