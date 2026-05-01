import json
import time
import random
from datetime import datetime, timezone
from typing import Dict, Any, Optional

import requests
import paho.mqtt.client as mqtt

# =============================================================
#  CONFIG  — بدّل هذا فقط
# =============================================================
BROKER = "localhost"
PORT   = 1883

BACKEND_BASE_URL   = "http://localhost:8081"
BACKEND_BINS_URL   = f"{BACKEND_BASE_URL}/api/bins"
BACKEND_LOGIN_URL  = f"{BACKEND_BASE_URL}/api/auth/login"

# بيانات حساب MUNICIPALITY للـ login التلقائي
LOGIN_EMAIL    = "admin@test.com"   # ← بدّل
LOGIN_PASSWORD = "admin123"          # ← بدّل

PUBLISH_EVERY_SEC      = 60    # كل دقيقة — كيف طلبتي
REFRESH_BINS_EVERY_SEC = 180   # تحديث قائمة الـ bins كل 3 دقائق

MQTT_SOURCE = "MQTT_SIM"

# =============================================================
#  TOKEN MANAGER  — يتجدد تلقائياً
# =============================================================
class TokenManager:
    def __init__(self):
        self._token: Optional[str] = None
        self._expires_at: float    = 0.0   # epoch seconds

    def get_token(self) -> str:
        # نجدد الـ token قبل 5 دقائق من انتهائه
        if self._token is None or time.time() > (self._expires_at - 300):
            self._refresh()
        return self._token

    def _refresh(self):
        print("[AUTH] Logging in to get fresh JWT token...")
        try:
            resp = requests.post(
                BACKEND_LOGIN_URL,
                json={"email": LOGIN_EMAIL, "password": LOGIN_PASSWORD},
                timeout=15
            )
            resp.raise_for_status()
            data = resp.json()

            self._token = data.get("token") or data.get("accessToken")
            if not self._token:
                raise ValueError(f"No token in response: {data}")

            # الـ JWT مدته في الـ exp claim — نقدر نـparse أو نحط ساعة كـ default
            # نحط 23 ساعة كـ safe margin
            self._expires_at = time.time() + (23 * 3600)
            print("[AUTH] Token refreshed successfully.")

        except Exception as e:
            print(f"[AUTH ERROR] Login failed: {e}")
            raise

    def get_headers(self) -> Dict[str, str]:
        return {
            "Accept":        "application/json",
            "Authorization": f"Bearer {self.get_token()}"
        }

token_manager = TokenManager()

# =============================================================
#  MQTT CLIENT
# =============================================================
mqtt_client = mqtt.Client(client_id=f"py-sim-all-bins-{random.randint(1000, 9999)}")

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print(f"[MQTT] Connected to broker {BROKER}:{PORT}")
    else:
        print(f"[MQTT] Connection failed, rc={rc}")

def on_disconnect(client, userdata, rc):
    if rc != 0:
        print(f"[MQTT] Unexpected disconnect rc={rc}, reconnecting...")
        while True:
            try:
                client.reconnect()
                print("[MQTT] Reconnected.")
                break
            except Exception as e:
                print(f"[MQTT] Reconnect failed: {e} — retrying in 5s")
                time.sleep(5)

mqtt_client.on_connect    = on_connect
mqtt_client.on_disconnect = on_disconnect
mqtt_client.connect(BROKER, PORT, keepalive=60)
mqtt_client.loop_start()

# =============================================================
#  BIN STATE
# =============================================================
bin_states: Dict[str, Dict[str, Any]] = {}
last_refresh_ts: float = 0.0


def fetch_bins_from_backend():
    resp = requests.get(
        BACKEND_BINS_URL,
        headers=token_manager.get_headers(),
        timeout=15
    )
    if resp.status_code in (401, 403):
        # token expired → force refresh و retry مرة واحدة
        print("[AUTH] Token rejected, forcing refresh...")
        token_manager._expires_at = 0
        resp = requests.get(
            BACKEND_BINS_URL,
            headers=token_manager.get_headers(),
            timeout=15
        )
    resp.raise_for_status()

    data = resp.json()
    if not isinstance(data, list):
        raise ValueError(f"Unexpected response: {data}")

    return [b for b in data if b.get("binCode") and b.get("isActive") is not False]


# ---- profiles ------------------------------------------------
def pick_profile(waste_type: Optional[str], zone_name: Optional[str]) -> Dict:
    waste = (waste_type or "").strip().upper()
    zone  = (zone_name  or "").strip().lower()

    if waste in ("GRAY", "GREEN"):
        profiles = [
            {"name": "gg-normal",  "fill_range": (20, 45), "battery_range": (65, 95),
             "increment_range": (2, 5), "event_prob": 0.06, "battery_drain_prob": 0.05},
            {"name": "gg-busy",    "fill_range": (35, 70), "battery_range": (55, 90),
             "increment_range": (3, 7), "event_prob": 0.10, "battery_drain_prob": 0.07},
        ]
        if zone == "grenelle":
            profiles.append(
                {"name": "gg-critical-grenelle", "fill_range": (60, 88),
                 "battery_range": (40, 80), "increment_range": (4, 9),
                 "event_prob": 0.14, "battery_drain_prob": 0.10}
            )
    elif waste == "WHITE":
        profiles = [
            {"name": "white-quiet",  "fill_range": (5,  25), "battery_range": (70, 100),
             "increment_range": (0, 2), "event_prob": 0.02, "battery_drain_prob": 0.03},
            {"name": "white-normal", "fill_range": (10, 35), "battery_range": (65, 95),
             "increment_range": (1, 3), "event_prob": 0.04, "battery_drain_prob": 0.04},
        ]
    elif waste == "YELLOW":
        profiles = [
            {"name": "yellow-normal", "fill_range": (10, 35), "battery_range": (65, 95),
             "increment_range": (1, 3), "event_prob": 0.04, "battery_drain_prob": 0.04},
            {"name": "yellow-busy",   "fill_range": (20, 50), "battery_range": (55, 90),
             "increment_range": (2, 4), "event_prob": 0.06, "battery_drain_prob": 0.05},
        ]
    else:
        profiles = [
            {"name": "fallback-normal", "fill_range": (15, 40), "battery_range": (60, 95),
             "increment_range": (1, 4), "event_prob": 0.05, "battery_drain_prob": 0.05},
        ]

    return random.choice(profiles)


def create_initial_state(bin_obj: Dict) -> Dict:
    waste_type = bin_obj.get("wasteType")
    zone_name  = bin_obj.get("zoneName")
    profile    = pick_profile(waste_type, zone_name)

    return {
        "id":                    bin_obj.get("id"),
        "fill":                  random.randint(*profile["fill_range"]),
        "battery":               random.randint(*profile["battery_range"]),
        "increment_range":       profile["increment_range"],
        "event_prob":            profile["event_prob"],
        "battery_drain_prob":    profile["battery_drain_prob"],
        "profile":               profile["name"],
        "wasteType":             waste_type,
        "zoneName":              zone_name,
        "is_full_since":         None,
    }


# ---- sync ----------------------------------------------------
def sync_bins():
    global bin_states
    try:
        backend_bins   = fetch_bins_from_backend()
        backend_codes  = set()

        for b in backend_bins:
            code = str(b["binCode"]).strip().upper()
            backend_codes.add(code)

            if code not in bin_states:
                bin_states[code] = create_initial_state(b)
                print(f"[NEW BIN] {code} | profile={bin_states[code]['profile']} | "
                      f"waste={bin_states[code]['wasteType']} | zone={bin_states[code]['zoneName']}")
            else:
                # تحديث الـ metadata بدون reset الـ fill
                bin_states[code].update({
                    "id":        b.get("id"),
                    "wasteType": b.get("wasteType"),
                    "zoneName":  b.get("zoneName"),
                })

        # احذف الـ bins اللي انحذفو من الـ DB
        for code in list(bin_states.keys()):
            if code not in backend_codes:
                print(f"[REMOVED BIN] {code}")
                del bin_states[code]

        print(f"[SYNC] Active bins in simulator: {len(bin_states)}")

    except Exception as e:
        print(f"[SYNC ERROR] {e}")


# ---- helpers -------------------------------------------------
def _period():
    h = datetime.now().hour
    if h < 6:  return "night"
    if h < 12: return "morning"
    if h < 18: return "afternoon"
    return "evening"

_PERIOD_MULT = {"night": 0.35, "morning": 0.85, "afternoon": 1.15, "evening": 1.35}
_ZONE_MULT   = {"grenelle": 1.20, "javel": 1.05, "necker": 0.95, "saint-lambert": 0.90}
_WASTE_MULT  = {"GRAY": 1.15, "GREEN": 1.15, "YELLOW": 0.95, "WHITE": 0.80}


def compute_increment(state: Dict) -> int:
    lo, hi  = state["increment_range"]
    base    = random.randint(lo, hi)
    period  = _period()
    waste   = (state.get("wasteType") or "").strip().upper()
    zone    = (state.get("zoneName")  or "").strip().lower()

    inc = base
    inc = round(inc * _PERIOD_MULT.get(period, 1.0))
    inc = round(inc * _ZONE_MULT.get(zone,   1.0))
    inc = round(inc * _WASTE_MULT.get(waste,  1.0))
    inc = max(0, inc)

    if random.random() < state["event_prob"]:
        bonus = random.randint(4, 12)
        inc  += bonus
        print(f"[EVENT] +{bonus} | profile={state['profile']} | waste={waste} | zone={zone}")

    return inc


def update_battery(state: Dict):
    if random.random() < state["battery_drain_prob"]:
        state["battery"] = max(5, state["battery"] - random.randint(1, 3))
    if state["battery"] < 25 and random.random() < 0.25:
        state["battery"] = max(3, state["battery"] - 1)


def compute_status(fill: int, battery: int) -> str:
    if battery <= 10: return "ERROR"
    if fill   >= 95:  return "FULL"
    if fill   >= 80:  return "WARNING"
    return "OK"


# ---- tick & publish ------------------------------------------
def simulation_tick():
    for code, state in list(bin_states.items()):

        # fill simulation
        if state["fill"] < 100:
            state["fill"] = min(100, state["fill"] + compute_increment(state))

        # auto-reset بعد وصول 100
        if state["fill"] >= 100:
            state["fill"] = 100
            if state["is_full_since"] is None:
                state["is_full_since"] = datetime.now(timezone.utc).isoformat()
                print(f"[FULL] {code} is full since {state['is_full_since']}")
        else:
            state["is_full_since"] = None

        update_battery(state)

        # بناء الـ payload
        payload = {
            "binCode":      code,
            "fillLevel":    state["fill"],
            "batteryLevel": state["battery"],
            "weightKg":     round((state["fill"] / 100.0) * random.uniform(8, 35), 2),
            "rssi":         random.randint(-95, -55),
            "status":       compute_status(state["fill"], state["battery"]),
            "source":       MQTT_SOURCE,
            "timestamp":    datetime.now(timezone.utc).isoformat(),
        }

        topic  = f"bins/{code}/telemetry"
        result = mqtt_client.publish(topic, json.dumps(payload), qos=1)

        print(
            f"[SEND] {code} | fill={state['fill']}% | battery={state['battery']}% | "
            f"status={payload['status']} | waste={state.get('wasteType')} | "
            f"zone={state.get('zoneName')} | rc={result.rc}"
        )


# =============================================================
#  MAIN LOOP
# =============================================================
def main():
    global last_refresh_ts

    print("=" * 60)
    print("  WISE TRASH — MQTT BIN TELEMETRY SIMULATOR")
    print(f"  Broker  : {BROKER}:{PORT}")
    print(f"  Backend : {BACKEND_BASE_URL}")
    print(f"  Publish : every {PUBLISH_EVERY_SEC}s")
    print(f"  Refresh : every {REFRESH_BINS_EVERY_SEC}s")
    print("=" * 60)

    # أول sync + أول telemetry فورياً
    sync_bins()
    last_refresh_ts = time.time()

    if not bin_states:
        print("[ERROR] No active bins found. Check backend & credentials.")
        return

    simulation_tick()   # أول بثّ فوري

    try:
        while True:
            time.sleep(PUBLISH_EVERY_SEC)

            now = time.time()

            # هل لازم نجدد قائمة الـ bins؟
            if now - last_refresh_ts >= REFRESH_BINS_EVERY_SEC:
                sync_bins()
                last_refresh_ts = now

            if not bin_states:
                print("[WARN] No active bins. Waiting...")
                continue

            simulation_tick()

    except KeyboardInterrupt:
        print("\n[STOP] Simulator stopped by user.")

    finally:
        mqtt_client.loop_stop()
        mqtt_client.disconnect()
        print("[MQTT] Disconnected.")


if __name__ == "__main__":
    main()