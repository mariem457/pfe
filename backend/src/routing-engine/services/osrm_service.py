import requests
from config.settings import OSRM_URL, OSRM_TIMEOUT


def call_osrm_table(locations):
    coords = ";".join([f"{lng},{lat}" for lat, lng in locations])

    response = requests.get(
        f"{OSRM_URL}/{coords}",
        params={"annotations": "distance,duration"},
        timeout=OSRM_TIMEOUT
    )

    response.raise_for_status()
    data = response.json()

    return data["distances"], data["durations"]


def call_osrm_route(locations):
    coords = ";".join([f"{lng},{lat}" for lat, lng in locations])

    response = requests.get(
        f"http://router.project-osrm.org/route/v1/driving/{coords}",
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