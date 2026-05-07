import requests
from config.settings import TOMTOM_API_KEY, TOMTOM_TIMEOUT

traffic_cache = {}


def get_traffic_delay_minutes(lat, lng):
    if not TOMTOM_API_KEY or not TOMTOM_API_KEY.strip():
        return 0

    cache_key = f"{round(lat, 3)}_{round(lng, 3)}"

    if cache_key in traffic_cache:
        return traffic_cache[cache_key]

    url = "https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json"

    params = {
        "point": f"{lat},{lng}",
        "unit": "KMPH",
        "key": TOMTOM_API_KEY
    }

    try:
        response = requests.get(url, params=params, timeout=TOMTOM_TIMEOUT)

        print("Traffic request URL:", response.url, flush=True)
        print("Traffic response code:", response.status_code, flush=True)
        print("Traffic raw response:", response.text[:1500], flush=True)

        response.raise_for_status()
        data = response.json()

        flow = data.get("flowSegmentData", {})

        current_time_sec = float(flow.get("currentTravelTime", 0) or 0)
        free_flow_time_sec = float(flow.get("freeFlowTravelTime", 0) or 0)
        road_closed = bool(flow.get("roadClosure", False))

        delay_sec = max(0, current_time_sec - free_flow_time_sec)
        delay_min = int(round(delay_sec / 60))

        if road_closed:
            delay_min += 20

        traffic_cache[cache_key] = delay_min

        print(
            f"REAL TRAFFIC => point=({lat},{lng}) "
            f"current={current_time_sec}s free={free_flow_time_sec}s "
            f"delay={delay_min} min roadClosed={road_closed}",
            flush=True
        )

        return delay_min

    except Exception as e:
        print(f"TOMTOM TRAFFIC FALLBACK => {e}", flush=True)
        traffic_cache[cache_key] = 0
        return 0