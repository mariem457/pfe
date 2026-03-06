import time
import random
import requests
import math

API = "http://localhost:8081/api/truck-locations"

TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZ2VudF9QYXJpcyIsInJvbGUiOiJNVU5JQ0lQQUxJVFkiLCJpYXQiOjE3NzI3MDYyMTksImV4cCI6MTc3Mjc5MjYxOX0._ACW-oLC848-dqwgo9HB8MC8yceLM3mbaJbQ_r7gJiA"

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

# نقاط داخل Paris 15
STARTS = [
    (48.8460, 2.2950),
    (48.8445, 2.2920),
    (48.8425, 2.2970),
    (48.8480, 2.2870),
]

ENDS = [
    (48.8405, 2.3000),
    (48.8472, 2.3050),
    (48.8430, 2.2855),
    (48.8458, 2.2795),
]


def compute_heading(lat1, lng1, lat2, lng2):
    dLon = math.radians(lng2 - lng1)

    lat1 = math.radians(lat1)
    lat2 = math.radians(lat2)

    y = math.sin(dLon) * math.cos(lat2)
    x = math.cos(lat1) * math.sin(lat2) - math.sin(lat1) * math.cos(lat2) * math.cos(dLon)

    brng = math.atan2(y, x)
    brng = math.degrees(brng)

    return (brng + 360) % 360


def get_route_points(lat1, lng1, lat2, lng2):
    url = OSRM_ROUTE.format(lat1=lat1, lng1=lng1, lat2=lat2, lng2=lng2)

    r = requests.get(url)
    data = r.json()

    coords = data["routes"][0]["geometry"]["coordinates"]

    return [(c[1], c[0]) for c in coords]


class Truck:

    def __init__(self, driver_id, route_points):
        self.driver_id = driver_id
        self.route = route_points
        self.i = 0

    def step(self):

        if self.i >= len(self.route) - 1:
            self.i = 0

        lat, lng = self.route[self.i]
        next_lat, next_lng = self.route[self.i + 1]

        heading = compute_heading(lat, lng, next_lat, next_lng)

        self.i += 1

        return lat, lng, heading


def post_location(driver_id, lat, lng, heading):

    speed = random.randint(25, 40)

    body = {
        "driverId": driver_id,
        "lat": lat,
        "lng": lng,
        "speedKmh": speed,
        "headingDeg": heading,
        "timestamp": int(time.time() * 1000)
    }

    r = requests.post(API, json=body, headers=HEADERS)

    if r.status_code >= 400:
        print("POST failed:", r.status_code, r.text)


def make_new_truck(driver_id):

    s = random.choice(STARTS)
    e = random.choice(ENDS)

    pts = get_route_points(s[0], s[1], e[0], e[1])

    return Truck(driver_id, pts)


def main():

    trucks = []

    for did in DRIVER_IDS:
        trucks.append(make_new_truck(did))

    print("Simulation running... CTRL+C to stop")

    while True:

        for t in trucks:

            lat, lng, heading = t.step()

            try:
                post_location(t.driver_id, lat, lng, heading)

            except Exception as e:
                print("Error:", e)

        time.sleep(1)


if __name__ == "__main__":
    main()