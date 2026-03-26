import math
from ortools.constraint_solver import pywrapcp, routing_enums_pb2
from services.matrix_service import create_matrices
from services.fuel_service import compute_max_distance_from_fuel, is_truck_eligible_by_fuel
from services.osrm_service import call_osrm_route
from models.routing_models import (
    RoutingRequestDto,
    RoutingResponseDto,
    RoutingMissionDto,
    RoutingStopDto,
    RouteCoordinateDto
)
from config.settings import DEFAULT_MAX_ROUTE_MINUTES


def filter_eligible_trucks(trucks):
    eligible = []

    for truck in trucks:
        status_ok = truck.status is None or truck.status.upper() == "AVAILABLE"
        fuel_ok = is_truck_eligible_by_fuel(truck)

        print(
            f"Truck {truck.id} -> status={truck.status}, status_ok={status_ok}, fuel_ok={fuel_ok}, maxDistance={compute_max_distance_from_fuel(truck)}",
            flush=True
        )

        if status_ok and fuel_ok:
            eligible.append(truck)

    return eligible


def optimize_routing(request: RoutingRequestDto) -> RoutingResponseDto:
    print(
        f"Optimizing with {len(request.bins)} bins and {len(request.trucks)} trucks",
        flush=True
    )

    eligible_trucks = filter_eligible_trucks(request.trucks)

    print(f"Eligible trucks after fuel/status filtering: {len(eligible_trucks)}", flush=True)

    if not eligible_trucks or not request.bins:
        print("No eligible trucks or no bins received. Returning empty missions.", flush=True)
        return RoutingResponseDto(missions=[], matrixSource="NONE")

    distance_matrix, duration_matrix, matrix_source = create_matrices(request)

    if len(distance_matrix) > 1 and len(distance_matrix[0]) > 1:
        print(
            f"Distance depot -> first point (m): {distance_matrix[0][1]}",
            flush=True
        )
        print(
            f"Duration depot -> first point (min): {duration_matrix[0][1]}",
            flush=True
        )

    for b in request.bins:
        print(
            f"Bin {b.id} -> fillLevel={b.fillLevel}, estimatedLoadKg={b.estimatedLoadKg}",
            flush=True
        )

    # minimum 1 باش capacity constraint تبقى فعالة
    demands = [0] + [max(1, int(round(b.estimatedLoadKg or 0))) for b in request.bins]

    capacities = [int(t.remainingCapacityKg) for t in eligible_trucks]
    max_distances = [compute_max_distance_from_fuel(t) for t in eligible_trucks]
    max_route_minutes = [DEFAULT_MAX_ROUTE_MINUTES for _ in eligible_trucks]

    print(f"Demands: {demands}", flush=True)
    print(f"Vehicle capacities: {capacities}", flush=True)
    print(f"Max distances from fuel (m): {max_distances}", flush=True)
    print(f"Max route durations (min): {max_route_minutes}", flush=True)

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

    # 1 stop لكل bac، و depot = 0
    def stop_callback(from_i):
        node = manager.IndexToNode(from_i)
        return 0 if node == 0 else 1

    distance_transit = routing.RegisterTransitCallback(distance_callback)
    duration_transit = routing.RegisterTransitCallback(duration_callback)
    demand = routing.RegisterUnaryTransitCallback(demand_callback)
    stop_transit = routing.RegisterUnaryTransitCallback(stop_callback)

    # Objective principal = minimiser durée réelle
    routing.SetArcCostEvaluatorOfAllVehicles(duration_transit)

    # Capacity
    routing.AddDimensionWithVehicleCapacity(
        demand,
        0,
        capacities,
        True,
        "Capacity"
    )

    # Distance fuel autonomy
    routing.AddDimensionWithVehicleCapacity(
        distance_transit,
        0,
        max_distances,
        True,
        "Distance"
    )

    # Time max route duration
    routing.AddDimensionWithVehicleCapacity(
        duration_transit,
        0,
        max_route_minutes,
        True,
        "Time"
    )

    # Stops balancing dimension
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

    # Balance total time بين trucks
    time_dimension.SetGlobalSpanCostCoefficient(100)

    # Balance عدد الباكات بين trucks
    stops_dimension.SetGlobalSpanCostCoefficient(1000)

    # target تقريبي لعدد stops لكل truck
    target_stops_per_truck = math.ceil(len(request.bins) / len(eligible_trucks))
    soft_upper_bound = target_stops_per_truck + 1

    print(
        f"Balancing target -> targetStopsPerTruck={target_stops_per_truck}, softUpperBound={soft_upper_bound}",
        flush=True
    )

    for v in range(len(eligible_trucks)):
        end_index = routing.End(v)

        # ن penalizi route اللي تتعدى target برشة
        stops_dimension.SetCumulVarSoftUpperBound(
            end_index,
            soft_upper_bound,
            10000
        )

        # نعاون solver ينقص الزمن النهائي والstops النهائية
        routing.AddVariableMinimizedByFinalizer(time_dimension.CumulVar(end_index))
        routing.AddVariableMinimizedByFinalizer(stops_dimension.CumulVar(end_index))

    params = pywrapcp.DefaultRoutingSearchParameters()
    params.first_solution_strategy = routing_enums_pb2.FirstSolutionStrategy.PATH_CHEAPEST_ARC
    params.local_search_metaheuristic = routing_enums_pb2.LocalSearchMetaheuristic.GUIDED_LOCAL_SEARCH
    params.time_limit.seconds = 10

    print("Solving OR-Tools routing problem...", flush=True)
    solution = routing.SolveWithParameters(params)

    if not solution:
        print("No OR-Tools solution found.", flush=True)
        return RoutingResponseDto(missions=[], matrixSource=matrix_source)

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
                selected_bin = request.bins[to_node - 1]

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
                print(
                    f"Route geometry loaded for truck {eligible_trucks[v].id}: {len(route_coordinates)} points",
                    flush=True
                )
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

            print(
                f"Mission created for truck {eligible_trucks[v].id}: "
                f"distance={mission.totalDistanceKm} km, "
                f"duration={mission.totalDurationMinutes} min, "
                f"stops={len(mission.stops)}, "
                f"geometryPoints={len(mission.routeCoordinates)}",
                flush=True
            )

    print(f"Returned missions: {len(missions)}", flush=True)
    return RoutingResponseDto(missions=missions, matrixSource=matrix_source)