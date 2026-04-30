from typing import Dict, List, Optional

from models.routing_models import (
    RoutingIncidentDto,
    RoutingTruckDto,
    ExcludedTruckDto,
    WarningTruckDto
)


BLOCKING_INCIDENT_TYPES = {
    "BREAKDOWN",
    "DRIVER_UNAVAILABLE",
    "REFUEL_REQUIRED"
}

WARNING_INCIDENT_TYPES = {
    "FUEL_LOW",
    "DELAY",
    "GPS_LOST",
    "TRAFFIC_BLOCK"
}


def build_truck_incident_map(active_incidents: List[RoutingIncidentDto]) -> Dict[int, List[RoutingIncidentDto]]:
    incidents_by_truck: Dict[int, List[RoutingIncidentDto]] = {}

    for incident in active_incidents or []:
        truck_id = incident.truckId
        if truck_id is None:
            continue

        incidents_by_truck.setdefault(truck_id, []).append(incident)

    return incidents_by_truck


def is_blocking_incident(incident: RoutingIncidentDto) -> bool:
    incident_type = (incident.type or "").upper()
    severity = (incident.severity or "").upper()

    if incident_type in BLOCKING_INCIDENT_TYPES:
        return True

    if incident_type == "OVERLOAD" and severity in ("HIGH", "CRITICAL"):
        return True

    return False


def is_warning_incident(incident: RoutingIncidentDto) -> bool:
    incident_type = (incident.type or "").upper()
    return incident_type in WARNING_INCIDENT_TYPES


def get_blocking_incident_reason(incidents: List[RoutingIncidentDto]) -> Optional[str]:
    for incident in incidents:
        if is_blocking_incident(incident):
            return f"{incident.type}:{incident.severity}"
    return None


def get_warning_reasons(incidents: List[RoutingIncidentDto]) -> List[str]:
    reasons: List[str] = []

    for incident in incidents:
        if is_warning_incident(incident):
            reasons.append(f"{incident.type}:{incident.severity}")

    return reasons


def filter_eligible_trucks(trucks: List[RoutingTruckDto], active_incidents: List[RoutingIncidentDto]):
    eligible: List[RoutingTruckDto] = []
    excluded: List[ExcludedTruckDto] = []
    warnings: List[WarningTruckDto] = []

    incidents_by_truck = build_truck_incident_map(active_incidents)

    for truck in trucks:
        status_ok = truck.status is None or truck.status.upper() == "AVAILABLE"

        truck_incidents = incidents_by_truck.get(truck.id, [])
        blocking_reason = get_blocking_incident_reason(truck_incidents)
        warning_reasons = get_warning_reasons(truck_incidents)

        exclusion_reason = None

        if not status_ok:
            exclusion_reason = f"STATUS:{truck.status}"
        elif blocking_reason is not None:
            exclusion_reason = blocking_reason

        print(
            f"Truck {truck.id} -> "
            f"status={truck.status}, status_ok={status_ok}, "
            f"activeIncidents={len(truck_incidents)}, "
            f"blockingReason={blocking_reason}, warningReasons={warning_reasons}, "
            f"exclusionReason={exclusion_reason}",
            flush=True
        )

        if exclusion_reason is None:
            eligible.append(truck)

            for reason in warning_reasons:
                warnings.append(
                    WarningTruckDto(
                        truckId=truck.id,
                        reason=reason
                    )
                )
        else:
            excluded.append(
                ExcludedTruckDto(
                    truckId=truck.id,
                    reason=exclusion_reason
                )
            )

    return eligible, excluded, warnings