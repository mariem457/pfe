from services.matrix_provider_service import get_matrix_from_provider
from services.traffic_service import get_traffic_delay_minutes
from config.settings import (
    DEFAULT_TRAFFIC_MODE,
    HEAVY_TRAFFIC_FACTOR,
    LIGHT_TRAFFIC_FACTOR
)


def build_locations(request):
    locations = [(request.depot.lat, request.depot.lng)]

    for b in request.bins:
        locations.append((b.lat, b.lng))

    return locations


def apply_traffic_factor(duration_matrix, traffic_mode):
    mode = (traffic_mode or DEFAULT_TRAFFIC_MODE).upper()
    factor = 1.0

    if mode == "HEAVY":
        factor = HEAVY_TRAFFIC_FACTOR
    elif mode == "LIGHT":
        factor = LIGHT_TRAFFIC_FACTOR

    adjusted = []

    for row in duration_matrix:
        adjusted_row = []

        for val in row:
            if val == 0:
                adjusted_row.append(0)
            else:
                adjusted_row.append(max(1, int(val * factor)))

        adjusted.append(adjusted_row)

    return adjusted


def apply_real_traffic_delay(duration_matrix, locations, traffic_mode):
    mode = (traffic_mode or DEFAULT_TRAFFIC_MODE).upper()

    if mode != "REAL":
        return duration_matrix

    adjusted = []

    for i, row in enumerate(duration_matrix):
        adjusted_row = []

        for j, val in enumerate(row):
            if val == 0:
                adjusted_row.append(0)
                continue

            # ناخذو traffic متاع destination point
            lat, lng = locations[j]
            delay_min = get_traffic_delay_minutes(lat, lng)

            adjusted_row.append(max(1, int(val + delay_min)))

        adjusted.append(adjusted_row)

    return adjusted


def create_matrices(request):
    locations = build_locations(request)

    try:
        print("Calling matrix provider service...", flush=True)

        distances, durations, provider_source = get_matrix_from_provider(locations)

        distance_matrix = []
        duration_matrix = []

        for i in range(len(distances)):
            d_row = []
            t_row = []

            for j in range(len(distances[i])):
                if distances[i][j] is None or durations[i][j] is None:
                    raise RuntimeError(
                        f"Matrix provider returned null value at [{i}][{j}]"
                    )

                distance_meters = int(distances[i][j])
                duration_seconds = float(durations[i][j])

                d_row.append(distance_meters)

                if distance_meters == 0:
                    t_row.append(0)
                else:
                    t_row.append(max(1, int(duration_seconds / 60)))

            distance_matrix.append(d_row)
            duration_matrix.append(t_row)

        # ✅ Apply traffic logic
        if provider_source == "OSRM":
            if (request.trafficMode or "").upper() == "REAL":
                duration_matrix = apply_real_traffic_delay(
                    duration_matrix,
                    locations,
                    request.trafficMode
                )
            else:
                duration_matrix = apply_traffic_factor(
                    duration_matrix,
                    request.trafficMode
                )

        # ✅ Logs + source
        traffic_enabled = (request.trafficMode or "").upper() == "REAL"

        print("Traffic enrichment enabled:", traffic_enabled, flush=True)
        print(f"Matrix source: {provider_source}", flush=True)

        if distance_matrix:
            print("Sample distance row:", distance_matrix[0][:5], flush=True)
            print("Sample duration row:", duration_matrix[0][:5], flush=True)

        final_source = provider_source
        if traffic_enabled:
            final_source = provider_source + " + TOMTOM_TRAFFIC"

        return distance_matrix, duration_matrix, final_source

    except Exception as e:
        print("Matrix provider failed:", e, flush=True)
        raise RuntimeError("Matrix provider failed - STOP optimization") from e