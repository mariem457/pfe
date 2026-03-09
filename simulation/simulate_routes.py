import time
import random
import requests
import math
import json
from pathlib import Path

API = "http://localhost:8081/api/truck-locations"

TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZ2VudF9QYXJpcyIsInJvbGUiOiJNVU5JQ0lQQUxJVFkiLCJpYXQiOjE3NzI4MjgxNzksImV4cCI6MTc3MjkxNDU3OX0.aGUJKcOf7NjJ4mZZLbfQpaaWKkJiaDZf7LIDmuGCQxs"

HEADERS = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {TOKEN}"
}

# drivers الموجودين في DB
DRIVER_IDS = [1, 2, 3, 5]

# OSRM routing
OSRM_ROUTE = (
    "http://router.project-osrm.org/route/v1/driving/"
    "{lng1},{lat1};{lng2},{lat2}?overview=full&geometries=geojson"
)

# -----------------------------
# START / END POINTS داخل Mahdia
# بدّلهم إذا حبيت حسب zone متاعك
# -----------------------------
STARTS = [
    (35.5048, 11.0622),
    (35.5065, 11.0588),
    (35.5019, 11.0651),
    (35.5092, 11.0604),
]

ENDS = [
    (35.4998, 11.0702),
    (35.5110, 11.0674),
    (35.5034, 11.0549),
    (35.4979, 11.0607),
]

# -----------------------------
# GeoJSON متاع Mahdia
# حط الملف هذا في نفس dossier متاع script
# -----------------------------
GEOJSON_PATH = Path(__file__).resolve().parent / "mahdia.geojson"

# أقصى عدد محاولات باش نلقى route كاملة داخل zone
MAX_ROUTE_ATTEMPTS = 25


def load_polygon_from_geojson(path: Path):
    with open(path, "r", encoding="utf-8") as f:
        geojson = json.load(f)

    geom = None

    if geojson.get("type") == "FeatureCollection":
        geom = geojson["features"][0]["geometry"]
    elif geojson.get("type") == "Feature":
        geom = geojson["geometry"]
    else:
        geom = geojson

    if not geom:
        raise ValueError("Invalid geojson: geometry not found")

    if geom["type"] == "Polygon":
        outer = geom["coordinates"][0]
        return [(p[1], p[0]) for p in outer]  # (lat, lng)

    if geom["type"] == "MultiPolygon":
        outer = geom["coordinates"][0][0]
        return [(p[1], p[0]) for p in outer]  # (lat, lng)

    raise ValueError(f"Unsupported geometry type: {geom['type']}")


MAHDIA_POLYGON = load_polygon_from_geojson(GEOJSON_PATH)


def point_in_polygon(lat, lng, polygon):
    inside = False
    n = len(polygon)

    for i in range(n):
        j = (i - 1) % n

        yi, xi = polygon[i]   # lat, lng
        yj, xj = polygon[j]   # lat, lng

        intersect = ((yi > lat) != (yj > lat)) and (
            lng < (xj - xi) * (lat - yi) / ((yj - yi) if (yj - yi) != 0 else 1e-12) + xi
        )

        if intersect:
            inside = not inside

    return inside


def route_inside_polygon(route_points, polygon):
    return all(point_in_polygon(lat, lng, polygon) for lat, lng in route_points)


def compute_heading(lat1, lng1, lat2, lng2):
    dlon = math.radians(lng2 - lng1)

    lat1 = math.radians(lat1)
    lat2 = math.radians(lat2)

    y = math.sin(dlon) * math.cos(lat2)
    x = math.cos(lat1) * math.sin(lat2) - math.sin(lat1) * math.cos(lat2) * math.cos(dlon)

    brng = math.atan2(y, x)
    brng = math.degrees(brng)

    return (brng + 360) % 360


def get_route_points(lat1, lng1, lat2, lng2):
    url = OSRM_ROUTE.format(lat1=lat1, lng1=lng1, lat2=lat2, lng2=lng2)

    r = requests.get(url, timeout=20)
    r.raise_for_status()

    data = r.json()

    routes = data.get("routes", [])
    if not routes:
        raise RuntimeError("OSRM returned no routes")

    coords = routes[0]["geometry"]["coordinates"]

    # رجّع (lat, lng)
    return [(c[1], c[0]) for c in coords]


class Truck:
    def __init__(self, driver_id, route_points):
        self.driver_id = driver_id
        self.route = route_points
        self.i = 0

    def step(self):
        if len(self.route) < 2:
            raise RuntimeError(f"Route too short for driver {self.driver_id}")

        if self.i >= len(self.route) - 1:
            self.i = 0

        lat, lng = self.route[self.i]
        next_lat, next_lng = self.route[self.i + 1]

        heading = compute_heading(lat, lng, next_lat, next_lng)

        self.i += 1

        return lat, lng, heading


def post_location(driver_id, lat, lng, heading):
    speed = random.randint(20, 40)

    body = {
        "driverId": driver_id,
        "lat": round(lat, 6),
        "lng": round(lng, 6),
        "speedKmh": speed,
        "headingDeg": round(heading, 2),
        "timestamp": int(time.time() * 1000)
    }

    r = requests.post(API, json=body, headers=HEADERS, timeout=20)

    if r.status_code >= 400:
        print("POST failed:", r.status_code, r.text)


def make_new_truck(driver_id):
    for attempt in range(1, MAX_ROUTE_ATTEMPTS + 1):
        s = random.choice(STARTS)
        e = random.choice(ENDS)

        try:
            pts = get_route_points(s[0], s[1], e[0], e[1])

            if len(pts) < 2:
                print(f"[driver {driver_id}] route too short, retry {attempt}/{MAX_ROUTE_ATTEMPTS}")
                continue

            if not route_inside_polygon(pts, MAHDIA_POLYGON):
                print(f"[driver {driver_id}] route left Mahdia zone, retry {attempt}/{MAX_ROUTE_ATTEMPTS}")
                continue

            print(f"[driver {driver_id}] valid Mahdia route found with {len(pts)} points")
            return Truck(driver_id, pts)

        except Exception as e:
            print(f"[driver {driver_id}] route error on attempt {attempt}: {e}")

    raise RuntimeError(f"Could not generate valid in-zone route for driver {driver_id}")


def main():
    print("Loading polygon from:", GEOJSON_PATH)
    print("Mahdia simulation running... CTRL+C to stop")

    trucks = [make_new_truck(did) for did in DRIVER_IDS]

    while True:
        for t in trucks:
            try:
                lat, lng, heading = t.step()
                post_location(t.driver_id, lat, lng, heading)
            except Exception as e:
                print(f"[driver {t.driver_id}] step/post error: {e}")
                try:
                    new_truck = make_new_truck(t.driver_id)
                    t.route = new_truck.route
                    t.i = 0
                    print(f"[driver {t.driver_id}] route regenerated")
                except Exception as regen_err:
                    print(f"[driver {t.driver_id}] regeneration failed: {regen_err}")

        time.sleep(1)


if __name__ == "__main__":
    main()