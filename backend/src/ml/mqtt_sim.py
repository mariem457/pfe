import json
import time
import random
import os
from pathlib import Path
from datetime import datetime, timezone

import paho.mqtt.client as mqtt
from dotenv import load_dotenv
from sqlalchemy import create_engine, text
from sqlalchemy.engine import URL


# =========================================================
# MQTT CONFIG
# =========================================================
BROKER = "localhost"   # إذا ما خدمش جرّبي "127.0.0.1"
PORT = 1883

# كل 5 دقائق
SEND_INTERVAL_SECONDS = 300

MIN_AFTER_COLLECTION = 5
MAX_AFTER_COLLECTION = 15

# إذا backend/driver سجّل collected=true في آخر 10 دقائق،
# simulation تعمل reset للـ fill
RECENT_COLLECTION_SECONDS = 600

DB_SYNC_ENABLED = True


# =========================================================
# DB CONFIG
# =========================================================
def find_env_file():
    current = Path(__file__).resolve().parent

    candidates = [
        current / ".env",
        Path(r"C:\Users\takwa\OneDrive\Bureau\pfe\backend\.env"),
        Path(r"C:\Users\takwa\OneDrive\Bureau\pfe\backend\src\.env"),
    ]

    for candidate in candidates:
        if candidate.exists():
            return candidate

    for parent in [current, *current.parents]:
        candidate = parent / ".env"
        if candidate.exists():
            return candidate

    return None


def create_db_engine():
    env_path = find_env_file()

    if env_path is None:
        raise FileNotFoundError(
            "No .env found. Put .env in backend folder or next to mqtt_sim.py."
        )

    load_dotenv(env_path)

    db_username = os.getenv("DB_USERNAME", "neondb_owner")
    db_password = os.getenv("DB_PASSWORD")

    if not db_password:
        raise ValueError("DB_PASSWORD not found in .env.")

    db_url = URL.create(
        drivername="postgresql+psycopg2",
        username=db_username,
        password=db_password,
        host="ep-dry-art-aldkcxqo-pooler.c-3.eu-central-1.aws.neon.tech",
        database="neondb",
        query={"sslmode": "require"},
    )

    print(f"DB connected. Loaded .env from: {env_path}")

    return create_engine(db_url)


engine = create_db_engine() if DB_SYNC_ENABLED else None


# =========================================================
# LOAD ALL BINS FROM DB
# =========================================================
def load_bin_codes_from_db():
    sql = text("""
        SELECT bin_code
        FROM bins
        WHERE bin_code IS NOT NULL
        ORDER BY id
    """)

    with engine.connect() as conn:
        rows = conn.execute(sql).fetchall()

    bin_codes = [row[0] for row in rows]

    if not bin_codes:
        raise ValueError("No bins found in DB table bins.")

    return bin_codes


BIN_CODES = load_bin_codes_from_db()

print(f"Loaded {len(BIN_CODES)} bins from DB:")
print(BIN_CODES)


# =========================================================
# MQTT CLIENT
# =========================================================
client = mqtt.Client(client_id="py-sim-all-db-bins")
client.connect(BROKER, PORT, 60)
client.loop_start()


# =========================================================
# STATE PER BIN
# =========================================================
bins_state = {}

for bin_code in BIN_CODES:
    bins_state[bin_code] = {
        "fill": random.randint(10, 35),
        "last_handled_collected_telemetry_id": None,
    }


# =========================================================
# HELPERS
# =========================================================
def get_activity_period():
    hour = datetime.now().hour

    if 0 <= hour < 6:
        return "night"
    elif 6 <= hour < 12:
        return "morning"
    elif 12 <= hour < 18:
        return "afternoon"
    else:
        return "evening"


def get_fill_increment(period):
    if period == "night":
        return random.choice([0, 0, 1])
    elif period == "morning":
        return random.choice([0, 1, 1, 2])
    elif period == "afternoon":
        return random.choice([1, 1, 2, 2, 3])
    else:
        return random.choice([1, 2, 2, 3])


def is_event():
    return random.random() < 0.03


def get_status(fill, collected=False):
    if collected:
        return "COLLECTED"

    if fill >= 95:
        return "FULL"
    elif fill >= 80:
        return "WARNING"
    else:
        return "OK"


def publish_telemetry(bin_code, fill, collected=False):
    topic = f"bins/{bin_code}/telemetry"
    status = get_status(fill, collected)

    payload = {
        "binCode": bin_code,
        "fillLevel": int(fill),
        "batteryLevel": random.randint(60, 100),
        "status": status,
        "source": "PY_SIM",
        "collected": bool(collected),
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }

    msg = json.dumps(payload)

    client.publish(topic, msg, qos=1)

    print(
        f"sent to {topic} | fill={fill}% | "
        f"status={status} | collected={collected}"
    )


def get_latest_db_telemetry(bin_code):
    if engine is None:
        return None

    sql = text("""
        SELECT
            bt.id,
            bt.fill_level,
            bt.collected,
            bt.timestamp
        FROM bin_telemetry bt
        JOIN bins b ON b.id = bt.bin_id
        WHERE b.bin_code = :bin_code
        ORDER BY bt.timestamp DESC, bt.id DESC
        LIMIT 1
    """)

    try:
        with engine.connect() as conn:
            row = conn.execute(sql, {"bin_code": bin_code}).mappings().first()
            return row
    except Exception as e:
        print(f"DB sync failed for {bin_code}: {e}")
        return None


def sync_collection_from_db(bin_code):
    state = bins_state[bin_code]

    latest = get_latest_db_telemetry(bin_code)

    if latest is None:
        return

    telemetry_id = latest["id"]
    collected = bool(latest["collected"])
    timestamp = latest["timestamp"]

    if timestamp is None:
        return

    if timestamp.tzinfo is None:
        timestamp = timestamp.replace(tzinfo=timezone.utc)

    age_seconds = (datetime.now(timezone.utc) - timestamp).total_seconds()

    if (
        collected
        and telemetry_id != state["last_handled_collected_telemetry_id"]
        and age_seconds <= RECENT_COLLECTION_SECONDS
    ):
        old_fill = state["fill"]
        new_fill = random.randint(MIN_AFTER_COLLECTION, MAX_AFTER_COLLECTION)

        state["fill"] = new_fill
        state["last_handled_collected_telemetry_id"] = telemetry_id

        print(
            f"DB COLLECTION DETECTED | {bin_code} | "
            f"telemetry_id={telemetry_id} | "
            f"reset fill {old_fill}% -> {new_fill}%"
        )


def simulate_one_bin(bin_code):
    state = bins_state[bin_code]

    sync_collection_from_db(bin_code)

    period = get_activity_period()
    increment = get_fill_increment(period)

    if is_event():
        increment += random.randint(2, 5)
        print(f"EVENT MODE for {bin_code}: extra waste detected")

    state["fill"] = max(state["fill"], min(100, state["fill"] + increment))

    publish_telemetry(bin_code, state["fill"], collected=False)

    if state["fill"] >= 100:
        print(f"{bin_code} FULL -> collection simulated")

        time.sleep(2)

        state["fill"] = random.randint(MIN_AFTER_COLLECTION, MAX_AFTER_COLLECTION)

        # هذه مهمة باش backend/model يفهم cycle جديد
        publish_telemetry(bin_code, state["fill"], collected=True)

        print(f"{bin_code} emptied, new fill level = {state['fill']}%")


# =========================================================
# MAIN LOOP
# =========================================================
try:
    print("Starting telemetry simulation for all DB bins")
    print(f"Total bins: {len(BIN_CODES)}")
    print(f"Send interval: {SEND_INTERVAL_SECONDS} seconds")

    while True:
        for bin_code in BIN_CODES:
            simulate_one_bin(bin_code)

            # pause صغيرة بين poubelle و poubelle
            time.sleep(1)

        print("---- simulation cycle done ----")
        time.sleep(SEND_INTERVAL_SECONDS)

except KeyboardInterrupt:
    print("Stopping...")

finally:
    client.loop_stop()
    client.disconnect()
