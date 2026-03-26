from models.routing_models import RoutingTruckDto
from config.settings import (
    DEFAULT_MAX_DISTANCE_METERS,
    FUEL_SAFETY_FACTOR,
    MIN_FUEL_RESERVE_LITERS,
    MIN_REQUIRED_DISTANCE_METERS
)


def compute_max_distance_from_fuel(truck: RoutingTruckDto) -> int:
    if truck.fuelLevelLiters is None or truck.fuelConsumptionPerKm is None:
        return DEFAULT_MAX_DISTANCE_METERS

    if truck.fuelConsumptionPerKm <= 0:
        return DEFAULT_MAX_DISTANCE_METERS

    usable_fuel_liters = truck.fuelLevelLiters - MIN_FUEL_RESERVE_LITERS
    if usable_fuel_liters <= 0:
        return 0

    autonomie_km = usable_fuel_liters / truck.fuelConsumptionPerKm
    usable_autonomie_km = autonomie_km * FUEL_SAFETY_FACTOR

    max_distance_meters = int(usable_autonomie_km * 1000)

    return max(0, max_distance_meters)


def is_truck_eligible_by_fuel(truck: RoutingTruckDto) -> bool:
    return compute_max_distance_from_fuel(truck) >= MIN_REQUIRED_DISTANCE_METERS