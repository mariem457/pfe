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
BROKER = "localhost"          # أو 127.0.0.1
PORT = 1883
BACKEND_BINS_URL = "http://localhost:8081/api/bins"
REFRESH_BINS_EVERY_SEC = 120   # كل قداش يعاود يجيب liste bins من backend
PUBLISH_EVERY_SEC = 15         # كل قداش يبعث telemetry

# source الأفضل يكون واحد من اللي backend يتقبلهم
# إذا backend عندك يقبل أي source تنجم تخليه PY_SIM
MQTT_SOURCE = "MQTT_SIM"

# =========================
# AUTH
# =========================
USE_AUTH = True
JWT_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZ2VudF9QYXJpcyIsInJvbGUiOiJNVU5JQ0lQQUxJVFkiLCJpYXQiOjE3NzQyNjA2MDYsImV4cCI6MTc3NDM0NzAwNn0.z8oX3iqvFV5AVq70XSjEBJoioImO_VPazGwNviUHYu0"

# =========================
# MQTT SETUP
# =========================
client = mqtt.Client(client_id=f"py-sim-all-bins-{random.randint(1000,9999)}")
client.connect(BROKER, PORT, 60)
client.loop_start()

# =========================
# STATE
# =========================
# bin_states[binCode] = {
#   "id": ...,
#   "fill": ...,
#   "battery": ...,
#   "base_increment_range": (min, max),
#   "event_probability": ...,
#   "battery_drain_probability": ...,
#   "profile": ...
# }
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

    # ناخذ فقط active bins واللي عندهم binCode
    bins = []
    for b in data:
        if not b.get("binCode"):
            continue
        if b.get("isActive") is False:
            continue
        bins.append(b)

    return bins


def pick_profile():
    profiles = [
        {
            "name": "quiet",
            "fill_range": (5, 30),
            "battery_range": (75, 100),
            "increment_range": (0, 2),
            "event_probability": 0.02,
            "battery_drain_probability": 0.03,
        },
        {
            "name": "normal",
            "fill_range": (15, 45),
            "battery_range": (60, 95),
            "increment_range": (1, 4),
            "event_probability": 0.05,
            "battery_drain_probability": 0.05,
        },
        {
            "name": "busy",
            "fill_range": (35, 70),
            "battery_range": (50, 90),
            "increment_range": (2, 6),
            "event_probability": 0.08,
            "battery_drain_probability": 0.08,
        },
        {
            "name": "critical-zone",
            "fill_range": (60, 90),
            "battery_range": (35, 70),
            "increment_range": (3, 8),
            "event_probability": 0.12,
            "battery_drain_probability": 0.10,
        },
    ]
    return random.choice(profiles)


def create_initial_state(bin_obj):
    profile = pick_profile()

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
        "zoneName": bin_obj.get("zoneName"),
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
                print(f"[NEW BIN] {code} | profile={bin_states[code]['profile']}")
            else:
                # نحدث metadata فقط ونخلي state متاع simulation كما هو
                bin_states[code]["id"] = b.get("id")
                bin_states[code]["lat"] = b.get("lat")
                bin_states[code]["lng"] = b.get("lng")
                bin_states[code]["type"] = b.get("type")
                bin_states[code]["zoneName"] = b.get("zoneName")

        # نحيو bins اللي ما عادش موجودة/active
        local_codes = list(bin_states.keys())
        for code in local_codes:
            if code not in backend_codes:
                print(f"[REMOVED BIN] {code} removed from simulator")
                del bin_states[code]

        print(f"[SYNC] active bins in simulator = {len(bin_states)}")

    except Exception as e:
        print(f"[SYNC ERROR] Impossible de charger les bins depuis backend: {e}")


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
        return 0.5
    if period == "morning":
        return 0.9
    if period == "afternoon":
        return 1.2
    return 1.4  # evening


def compute_increment(state: Dict[str, Any], period: str) -> int:
    min_inc, max_inc = state["base_increment_range"]
    base = random.randint(min_inc, max_inc)
    inc = max(0, round(base * period_multiplier(period)))

    # Event exceptionnel
    if random.random() < state["event_probability"]:
        bonus = random.randint(4, 12)
        inc += bonus
        print(f"[EVENT] extra waste for bin profile={state['profile']} (+{bonus})")

    return inc


def update_battery(state: Dict[str, Any]):
    # Drain faible normal
    if random.random() < state["battery_drain_probability"]:
        state["battery"] = max(5, state["battery"] - random.randint(1, 3))

    # Si batterie déjà basse، تنقص أسرع شوية
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


def maybe_simulate_collection(state: Dict[str, Any], code: str):
    # إذا تعبا برشة، بعد cycle يفرغ
    if state["fill"] >= 100:
        print(f"[COLLECTION] {code} full -> simulated emptying")
        state["fill"] = random.randint(5, 18)


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

    # إذا backend متاعك يقبلهم، تنجم تزيدهم:
    # payload["weightKg"] = round((state["fill"] / 100.0) * random.uniform(8, 35), 2)
    # payload["rssi"] = random.randint(-95, -55)

    msg = json.dumps(payload)
    result = client.publish(topic, msg, qos=1)

    print(
        f"[SEND] {bin_code} | "
        f"fill={state['fill']}% | "
        f"battery={state['battery']}% | "
        f"status={payload['status']} | "
        f"topic={topic} | rc={result.rc}"
    )


def simulation_tick():
    period = get_activity_period()

    for code, state in bin_states.items():
        increment = compute_increment(state, period)
        state["fill"] = min(100, state["fill"] + increment)

        update_battery(state)

        publish_bin_telemetry(code, state)

        maybe_simulate_collection(state, code)


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