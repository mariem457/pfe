from utils.geo_utils import haversine_distance_km
from services.matrix_provider_service import get_matrix_from_provider
from config.settings import (
    FALLBACK_SPEED_KMH,
    DEFAULT_TRAFFIC_MODE,
    HEAVY_TRAFFIC_FACTOR,
    LIGHT_TRAFFIC_FACTOR
)


def build_locations(request):
    locations = [(request.depot.lat, request.depot.lng)]
    for b in request.bins:
        locations.append((b.lat, b.lng))
    return locations


def fallback_matrix(locations):
    matrix = []

    for i in range(len(locations)):
        row = []
        for j in range(len(locations)):
            if i == j:
                row.append(0)
            else:
                dist_km = haversine_distance_km(
                    locations[i][0], locations[i][1],
                    locations[j][0], locations[j][1]
                )
                row.append(int(dist_km * 1000))
        matrix.append(row)

    return matrix


def duration_from_distance(distance_matrix):
    duration = []

    for row in distance_matrix:
        d_row = []
        for d in row:
            if d == 0:
                d_row.append(0)
            else:
                km = d / 1000
                minutes = (km / FALLBACK_SPEED_KMH) * 60
                d_row.append(max(1, int(minutes)))
        duration.append(d_row)

    return duration


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
                    fallback_km = haversine_distance_km(
                        locations[i][0], locations[i][1],
                        locations[j][0], locations[j][1]
                    )
                    fallback_meters = int(fallback_km * 1000)
                    fallback_minutes = max(1, int((fallback_km / FALLBACK_SPEED_KMH) * 60)) if fallback_meters > 0 else 0

                    d_row.append(fallback_meters)
                    t_row.append(fallback_minutes)
                else:
                    d_row.append(int(distances[i][j]))

                    if int(distances[i][j]) == 0:
                        t_row.append(0)
                    else:
                        t_row.append(max(1, int(durations[i][j] / 60)))

            distance_matrix.append(d_row)
            duration_matrix.append(t_row)

        if provider_source == "OSRM":
            duration_matrix = apply_traffic_factor(duration_matrix, request.trafficMode)

        print(f"Matrix source: {provider_source}", flush=True)
        if len(distance_matrix) > 0:
            print("Sample distance row:", distance_matrix[0][:5], flush=True)
            print("Sample duration row:", duration_matrix[0][:5], flush=True)

        return distance_matrix, duration_matrix, provider_source

    except Exception as e:
        print("Matrix provider failed:", e, flush=True)

        dist = fallback_matrix(locations)
        dur = duration_from_distance(dist)
        dur = apply_traffic_factor(dur, request.trafficMode)

        print("Matrix source: HAVERSINE FALLBACK", flush=True)
        if len(dist) > 0:
            print("Sample distance row:", dist[0][:5], flush=True)
            print("Sample duration row:", dur[0][:5], flush=True)

        return dist, dur, "FALLBACK"