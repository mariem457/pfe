from models.routing_models import RoutingTruckDto


def compute_max_distance_from_fuel(truck: RoutingTruckDto) -> int:
    if truck.fuelLevelLiters is None or truck.fuelConsumptionPerKm is None:
        return 50000

    if truck.fuelConsumptionPerKm <= 0:
        return 50000

    autonomie_km = truck.fuelLevelLiters / truck.fuelConsumptionPerKm
    usable_autonomie_km = autonomie_km * 0.8

    return int(usable_autonomie_km * 1000)