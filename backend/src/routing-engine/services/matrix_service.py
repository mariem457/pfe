from utils.geo_utils import euclidean_distance_km
from services.osrm_service import call_osrm_table
from config.settings import FALLBACK_SPEED_KMH


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
                dist = euclidean_distance_km(
                    locations[i][0], locations[i][1],
                    locations[j][0], locations[j][1]
                )
                row.append(int(dist * 1000))
        matrix.append(row)
    return matrix


def duration_from_distance(distance_matrix):
    duration = []
    for row in distance_matrix:
        d_row = []
        for d in row:
            km = d / 1000
            minutes = (km / FALLBACK_SPEED_KMH) * 60
            d_row.append(int(minutes))
        duration.append(d_row)
    return duration


def create_matrices(request):
    locations = build_locations(request)

    try:
        print("Calling OSRM table service...", flush=True)
        distances, durations = call_osrm_table(locations)

        distance_matrix = []
        duration_matrix = []

        for i in range(len(distances)):
            d_row = []
            t_row = []
            for j in range(len(distances[i])):
                if distances[i][j] is None or durations[i][j] is None:
                    fallback = euclidean_distance_km(
                        locations[i][0], locations[i][1],
                        locations[j][0], locations[j][1]
                    )
                    d_row.append(int(fallback * 1000))
                    t_row.append(int((fallback / FALLBACK_SPEED_KMH) * 60))
                else:
                    d_row.append(int(distances[i][j]))
                    t_row.append(int(durations[i][j] / 60))
            distance_matrix.append(d_row)
            duration_matrix.append(t_row)

        print("Matrix source: OSRM", flush=True)
        if len(distance_matrix) > 0:
            print("Sample distance row:", distance_matrix[0][:5], flush=True)
            print("Sample duration row:", duration_matrix[0][:5], flush=True)

        return distance_matrix, duration_matrix

    except Exception as e:
        print("OSRM failed:", e, flush=True)

        dist = fallback_matrix(locations)
        dur = duration_from_distance(dist)

        print("Matrix source: EUCLIDEAN FALLBACK", flush=True)
        if len(dist) > 0:
            print("Sample distance row:", dist[0][:5], flush=True)
            print("Sample duration row:", dur[0][:5], flush=True)

        return dist, dur