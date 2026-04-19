import json
import time
import random
from datetime import datetime, timezone
from typing import Dict, Any

import requests
import paho.mqtt.client as mqtt

# =========================
# CONFIG
# =========================
BROKER = "localhost"
PORT = 1883

BACKEND_BINS_URL = "http://localhost:8081/api/bins"

REFRESH_BINS_EVERY_SEC = 180
PUBLISH_EVERY_SEC = 30

MQTT_SOURCE = "MQTT_SIM"

# =========================
# AUTH
# =========================
USE_AUTH = True
JWT_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtYXJpZW1oYW1kaTAyMUBnbWFpbC5jb20iLCJyb2xlIjoiTVVOSUNJUEFMSVRZIiwidG9rZW5WZXJzaW9uIjowLCJpYXQiOjE3NzY1MDcxOTMsImV4cCI6MTc3NjU5MzU5M30.jmpwQaUIGbLjhU7IU9XPOxBjzE9L5NvjOhZNUU12OY4"

# =========================
# MQTT SETUP
# =========================
client = mqtt.Client(client_id=f"py-sim-all-bins-{random.randint(1000,9999)}")
client.connect(BROKER, PORT, 60)
client.loop_start()

# =========================
# STATE
# =========================
bin_states: Dict[str, Dict[str, Any]] = {}
last_refresh_ts = 0


def get_headers():
    headers = {
        "Accept": "application/json"
    }

    if USE_AUTH:
        token = JWT_TOKEN.strip()
        if not token:
            raise Exception("JWT_TOKEN vide. Colle ton Bearer token dans JWT_TOKEN.")
        headers["Authorization"] = f"Bearer {token}"

    return headers


def fetch_bins_from_backend():
    resp = requests.get(BACKEND_BINS_URL, headers=get_headers(), timeout=15)

    if resp.status_code == 401:
        raise Exception("401 Unauthorized -> token invalide ou expiré.")
    if resp.status_code == 403:
        raise Exception("403 Forbidden -> ce token n'a pas accès à /api/bins.")

    resp.raise_for_status()
    data = resp.json()

    if not isinstance(data, list):
        raise Exception(f"Réponse inattendue depuis backend: {data}")

    bins = []
    for b in data:
        if not b.get("binCode"):
            continue
        if b.get("isActive") is False:
            continue
        bins.append(b)

    return bins


def get_activity_period():
    hour = datetime.now().hour
    if 0 <= hour < 6:
        return "night"
    elif 6 <= hour < 12:
        return "morning"
    elif 12 <= hour < 18:
        return "afternoon"
    return "evening"


def period_multiplier(period: str) -> float:
    if period == "night":
        return 0.35
    if period == "morning":
        return 0.85
    if period == "afternoon":
        return 1.15
    return 1.35


def zone_multiplier(zone_name: str | None) -> float:
    if not zone_name:
        return 1.0

    z = zone_name.strip().lower()

    if z == "grenelle":
        return 1.20
    if z == "javel":
        return 1.05
    if z == "necker":
        return 0.95
    if z == "saint-lambert":
        return 0.90

    return 1.0


def pick_profile(waste_type: str | None, zone_name: str | None):
    waste = (waste_type or "").strip().upper()
    zone = (zone_name or "").strip().lower()

    if waste in ("GRAY", "GREEN"):
        profiles = [
            {
                "name": "gg-normal",
                "fill_range": (20, 45),
                "battery_range": (65, 95),
                "increment_range": (2, 5),
                "event_probability": 0.06,
                "battery_drain_probability": 0.05,
            },
            {
                "name": "gg-busy",
                "fill_range": (35, 70),
                "battery_range": (55, 90),
                "increment_range": (3, 7),
                "event_probability": 0.10,
                "battery_drain_probability": 0.07,
            },
        ]

        if zone == "grenelle":
            profiles.append(
                {
                    "name": "gg-critical-grenelle",
                    "fill_range": (60, 88),
                    "battery_range": (40, 80),
                    "increment_range": (4, 9),
                    "event_probability": 0.14,
                    "battery_drain_probability": 0.10,
                }
            )

    elif waste == "WHITE":
        profiles = [
            {
                "name": "white-quiet",
                "fill_range": (5, 25),
                "battery_range": (70, 100),
                "increment_range": (0, 2),
                "event_probability": 0.02,
                "battery_drain_probability": 0.03,
            },
            {
                "name": "white-normal",
                "fill_range": (10, 35),
                "battery_range": (65, 95),
                "increment_range": (1, 3),
                "event_probability": 0.04,
                "battery_drain_probability": 0.04,
            },
        ]

    elif waste == "YELLOW":
        profiles = [
            {
                "name": "yellow-normal",
                "fill_range": (10, 35),
                "battery_range": (65, 95),
                "increment_range": (1, 3),
                "event_probability": 0.04,
                "battery_drain_probability": 0.04,
            },
            {
                "name": "yellow-busy",
                "fill_range": (20, 50),
                "battery_range": (55, 90),
                "increment_range": (2, 4),
                "event_probability": 0.06,
                "battery_drain_probability": 0.05,
            },
        ]

    else:
        profiles = [
            {
                "name": "fallback-normal",
                "fill_range": (15, 40),
                "battery_range": (60, 95),
                "increment_range": (1, 4),
                "event_probability": 0.05,
                "battery_drain_probability": 0.05,
            }
        ]

    return random.choice(profiles)


def create_initial_state(bin_obj):
    waste_type = bin_obj.get("wasteType")
    zone_name = bin_obj.get("zoneName")

    profile = pick_profile(waste_type, zone_name)

    return {
        "id": bin_obj.get("id"),
        "fill": random.randint(*profile["fill_range"]),
        "battery": random.randint(*profile["battery_range"]),
        "base_increment_range": profile["increment_range"],
        "event_probability": profile["event_probability"],
        "battery_drain_probability": profile["battery_drain_probability"],
        "profile": profile["name"],
        "lat": bin_obj.get("lat"),
        "lng": bin_obj.get("lng"),
        "type": bin_obj.get("type"),
        "zoneName": zone_name,
        "wasteType": waste_type,
        "is_full_since": None,
    }


def sync_bins_with_backend():
    global bin_states

    try:
        backend_bins = fetch_bins_from_backend()
        backend_codes = set()

        for b in backend_bins:
            code = str(b["binCode"]).strip().upper()
            backend_codes.add(code)

            if code not in bin_states:
                bin_states[code] = create_initial_state(b)
                print(
                    f"[NEW BIN] {code} | "
                    f"profile={bin_states[code]['profile']} | "
                    f"waste={bin_states[code]['wasteType']} | "
                    f"zone={bin_states[code]['zoneName']}"
                )
            else:
                bin_states[code]["id"] = b.get("id")
                bin_states[code]["lat"] = b.get("lat")
                bin_states[code]["lng"] = b.get("lng")
                bin_states[code]["type"] = b.get("type")
                bin_states[code]["zoneName"] = b.get("zoneName")
                bin_states[code]["wasteType"] = b.get("wasteType")

        local_codes = list(bin_states.keys())
        for code in local_codes:
            if code not in backend_codes:
                print(f"[REMOVED BIN] {code} removed from simulator")
                del bin_states[code]

        print(f"[SYNC] active bins in simulator = {len(bin_states)}")

    except Exception as e:
        print(f"[SYNC ERROR] Impossible de charger les bins depuis backend: {e}")


def compute_increment(state: Dict[str, Any], period: str) -> int:
    min_inc, max_inc = state["base_increment_range"]
    base = random.randint(min_inc, max_inc)

    waste = (state.get("wasteType") or "").strip().upper()
    zone = state.get("zoneName")

    waste_mult = 1.0
    if waste in ("GRAY", "GREEN"):
        waste_mult = 1.15
    elif waste == "YELLOW":
        waste_mult = 0.95
    elif waste == "WHITE":
        waste_mult = 0.80

    inc = base
    inc = round(inc * period_multiplier(period))
    inc = round(inc * zone_multiplier(zone))
    inc = round(inc * waste_mult)
    inc = max(0, inc)

    if random.random() < state["event_probability"]:
        bonus = random.randint(4, 12)
        inc += bonus
        print(
            f"[EVENT] extra waste | "
            f"profile={state['profile']} | waste={waste} | zone={zone} | +{bonus}"
        )

    return inc


def update_battery(state: Dict[str, Any]):
    if random.random() < state["battery_drain_probability"]:
        state["battery"] = max(5, state["battery"] - random.randint(1, 3))

    if state["battery"] < 25 and random.random() < 0.25:
        state["battery"] = max(3, state["battery"] - 1)


def compute_status(fill: int, battery: int) -> str:
    if battery <= 10:
        return "ERROR"
    if fill >= 95:
        return "FULL"
    if fill >= 80:
        return "WARNING"
    return "OK"


def update_full_state(state: Dict[str, Any]):
    if state["fill"] >= 100:
        state["fill"] = 100
        if state["is_full_since"] is None:
            state["is_full_since"] = datetime.now(timezone.utc).isoformat()
    else:
        state["is_full_since"] = None


def publish_bin_telemetry(bin_code: str, state: Dict[str, Any]):
    topic = f"bins/{bin_code}/telemetry"

    payload = {
        "binCode": bin_code,
        "fillLevel": state["fill"],
        "batteryLevel": state["battery"],
        "status": compute_status(state["fill"], state["battery"]),
        "source": MQTT_SOURCE,
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }

    # إذا backend يقبلهم خلّيهم، وإذا لا علّقهم
    payload["weightKg"] = round((state["fill"] / 100.0) * random.uniform(8, 35), 2)
    payload["rssi"] = random.randint(-95, -55)

    msg = json.dumps(payload)
    result = client.publish(topic, msg, qos=1)

    print(
        f"[SEND] {bin_code} | "
        f"waste={state.get('wasteType')} | "
        f"zone={state.get('zoneName')} | "
        f"fill={state['fill']}% | "
        f"battery={state['battery']}% | "
        f"status={payload['status']} | "
        f"topic={topic} | rc={result.rc}"
    )


def simulation_tick():
    period = get_activity_period()

    for code, state in bin_states.items():
        increment = compute_increment(state, period)

        if state["fill"] < 100:
            state["fill"] = min(100, state["fill"] + increment)

        update_battery(state)
        update_full_state(state)
        publish_bin_telemetry(code, state)


def main():
    global last_refresh_ts

    print("=== MQTT BIN TELEMETRY SIMULATOR (ALL ACTIVE BINS FROM DB) ===")
    print(f"Broker: {BROKER}:{PORT}")
    print(f"Backend bins URL: {BACKEND_BINS_URL}")
    print(f"Publish every: {PUBLISH_EVERY_SEC}s")
    print(f"Refresh bins every: {REFRESH_BINS_EVERY_SEC}s")
    print(f"Auth enabled: {USE_AUTH}")
    print("Starting...\n")

    sync_bins_with_backend()
    last_refresh_ts = time.time()

    try:
        while True:
            now = time.time()

            if now - last_refresh_ts >= REFRESH_BINS_EVERY_SEC:
                sync_bins_with_backend()
                last_refresh_ts = now

            if not bin_states:
                print("[INFO] No active bins found. Retrying...")
                time.sleep(5)
                continue

            simulation_tick()
            time.sleep(PUBLISH_EVERY_SEC)

    except KeyboardInterrupt:
        print("\nStopping simulator...")

    finally:
        client.loop_stop()
        client.disconnect()
        print("Disconnected from MQTT.")


if __name__ == "__main__":
    main()