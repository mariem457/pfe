import requests
from config.settings import OSRM_TABLE_URL, OSRM_ROUTE_URL, OSRM_TIMEOUT


def call_osrm_table(locations):
    coords = ";".join([f"{lng},{lat}" for lat, lng in locations])

    response = requests.get(
        f"{OSRM_TABLE_URL}/{coords}",
        params={"annotations": "distance,duration"},
        timeout=OSRM_TIMEOUT
    )

    response.raise_for_status()
    data = response.json()

    return data["distances"], data["durations"]


def get_osrm_matrix(locations):
    distances, durations = call_osrm_table(locations)
    return distances, durations, "OSRM"


def call_osrm_route(locations):
    coords = ";".join([f"{lng},{lat}" for lat, lng in locations])

    response = requests.get(
        f"{OSRM_ROUTE_URL}/{coords}",
        params={
            "overview": "full",
            "geometries": "geojson"
        },
        timeout=OSRM_TIMEOUT
    )

    response.raise_for_status()
    data = response.json()

    routes = data.get("routes", [])
    if not routes:
        return []

    geometry = routes[0].get("geometry", {})
    coordinates = geometry.get("coordinates", [])

    return [{"lat": c[1], "lng": c[0]} for c in coordinates]








def call_osrm_route_summary(locations):
    coords = ";".join([f"{lng},{lat}" for lat, lng in locations])

    response = requests.get(
        f"{OSRM_ROUTE_URL}/{coords}",
        params={
            "overview": "false",
            "geometries": "geojson"
        },
        timeout=OSRM_TIMEOUT
    )

    response.raise_for_status()
    data = response.json()

    routes = data.get("routes", [])
    if not routes:
        return 0, 0

    route = routes[0]
    distance_meters = int(route.get("distance", 0))
    duration_minutes = max(1, int(route.get("duration", 0) / 60))

    return distance_meters, duration_minutes