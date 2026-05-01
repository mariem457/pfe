import time
import requests
from config.settings import TOMTOM_API_KEY, TOMTOM_TIMEOUT


TOMTOM_ASYNC_SUBMIT_URL = "https://api.tomtom.com/routing/matrix/2/async"
TOMTOM_ASYNC_STATUS_URL = "https://api.tomtom.com/routing/matrix/2/async/{job_id}"
TOMTOM_ASYNC_RESULT_URL = "https://api.tomtom.com/routing/matrix/2/async/{job_id}/result"

TOMTOM_POLL_INTERVAL_SECONDS = 2
TOMTOM_MAX_WAIT_SECONDS = 30


def _build_points(locations):
    points = []
    for lat, lng in locations:
        points.append({
            "point": {
                "latitude": lat,
                "longitude": lng
            }
        })
    return points


def submit_tomtom_async_matrix(locations):
    if not TOMTOM_API_KEY or not TOMTOM_API_KEY.strip():
        raise RuntimeError("TOMTOM_API_KEY is missing")

    body = {
        "origins": _build_points(locations),
        "destinations": _build_points(locations),
        "options": {
            "routeType": "fastest",
            "traffic": "live",
            "travelMode": "car",
            "departAt": "now"
        }
    }

    response = requests.post(
        TOMTOM_ASYNC_SUBMIT_URL,
        params={"key": TOMTOM_API_KEY},
        json=body,
        headers={"Content-Type": "application/json"},
        timeout=TOMTOM_TIMEOUT
    )

    print("TomTom async submit status code:", response.status_code, flush=True)
    print("TomTom async submit response:", response.text, flush=True)

    response.raise_for_status()
    data = response.json()

    job_id = data.get("jobId")
    if not job_id:
        raise RuntimeError(f"TomTom async submission did not return jobId: {data}")

    return job_id


def get_tomtom_async_status(job_id):
    response = requests.get(
        TOMTOM_ASYNC_STATUS_URL.format(job_id=job_id),
        params={"key": TOMTOM_API_KEY},
        timeout=TOMTOM_TIMEOUT
    )

    print("TomTom async status code:", response.status_code, flush=True)
    print("TomTom async status response:", response.text, flush=True)

    response.raise_for_status()
    return response.json()


def download_tomtom_async_matrix(job_id):
    response = requests.get(
        TOMTOM_ASYNC_RESULT_URL.format(job_id=job_id),
        params={"key": TOMTOM_API_KEY},
        headers={"Accept-Encoding": "gzip"},
        timeout=TOMTOM_TIMEOUT,
        allow_redirects=True
    )

    print("TomTom async result status code:", response.status_code, flush=True)
    print("TomTom async result response:", response.text[:1000], flush=True)

    response.raise_for_status()
    data = response.json()

    rows = data.get("data", [])
    if not rows:
        raise RuntimeError(f"TomTom async result returned no data: {data}")

    return rows


def wait_for_tomtom_async_completion(job_id):
    elapsed = 0

    while elapsed < TOMTOM_MAX_WAIT_SECONDS:
        status_data = get_tomtom_async_status(job_id)

        state = status_data.get("state") or status_data.get("status")
        print(f"TomTom async job state: {state}", flush=True)

        if state == "Completed":
            return

        if state in ("Failed", "Cancelled"):
            raise RuntimeError(f"TomTom async job ended with state={state}")

        time.sleep(TOMTOM_POLL_INTERVAL_SECONDS)
        elapsed += TOMTOM_POLL_INTERVAL_SECONDS

    raise TimeoutError(
        f"TomTom async job did not complete within {TOMTOM_MAX_WAIT_SECONDS} seconds"
    )


def get_tomtom_matrix(locations):
    job_id = submit_tomtom_async_matrix(locations)
    print(f"TomTom async job id: {job_id}", flush=True)

    wait_for_tomtom_async_completion(job_id)
    rows = download_tomtom_async_matrix(job_id)

    size = len(locations)
    distances = [[0 for _ in range(size)] for _ in range(size)]
    durations = [[0 for _ in range(size)] for _ in range(size)]

    for item in rows:
        origin_index = item["originIndex"]
        destination_index = item["destinationIndex"]
        route_summary = item.get("routeSummary", {})

        distances[origin_index][destination_index] = route_summary.get("lengthInMeters", 0)
        durations[origin_index][destination_index] = route_summary.get("travelTimeInSeconds", 0)

    return distances, durations, "TOMTOM"