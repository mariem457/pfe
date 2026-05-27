# =========================================================
# PREDICT ONE TELEMETRY FROM DB — NEON
# Usage:
# python predict_one_from_db.py <telemetry_id>
#
# FINAL CLEAN VERSION:
# - XGBoost predicts only predicted_fill_next
# - predicted_hours is calculated using realistic stable rate
# - raw telemetry can be every 5 minutes
# - hourly resampling is used only for XGBoost lag features
# =========================================================

import os
import sys
import joblib
import numpy as np
import pandas as pd

from pathlib import Path
from dotenv import load_dotenv
from sqlalchemy import create_engine, text
from sqlalchemy.engine import URL


# =========================================================
# ARGUMENT
# =========================================================
if len(sys.argv) < 2:
    raise ValueError("Missing telemetry_id. Usage: python predict_one_from_db.py <telemetry_id>")

TELEMETRY_ID = int(sys.argv[1])


# =========================================================
# PATH CONFIG
# =========================================================
SCRIPT_DIR = Path(__file__).resolve().parent

ENV_PATH = None

for parent in [SCRIPT_DIR, *SCRIPT_DIR.parents]:
    candidate = parent / ".env"
    if candidate.exists():
        ENV_PATH = candidate
        break

if ENV_PATH is None:
    raise FileNotFoundError("No .env file found. Put .env in backend folder.")

load_dotenv(ENV_PATH)


# =========================================================
# DATABASE CONFIG — NEON
# =========================================================
DB_HOST = "ep-dry-art-aldkcxqo-pooler.c-3.eu-central-1.aws.neon.tech"
DB_NAME = "neondb"

DB_USERNAME = os.getenv("DB_USERNAME", "neondb_owner")
DB_PASSWORD = os.getenv("DB_PASSWORD")

if not DB_PASSWORD:
    raise ValueError("DB_PASSWORD not found in .env")

DB_URL = URL.create(
    drivername="postgresql+psycopg2",
    username=DB_USERNAME,
    password=DB_PASSWORD,
    host=DB_HOST,
    database=DB_NAME,
    query={"sslmode": "require"},
)


# =========================================================
# MODEL CONFIG
# =========================================================
TIME_MAX = 48.0
TARGET_FILL_LEVEL = 90.0
TELEMETRY_INTERVAL_HOURS = 5.0 / 60.0
MIN_REALISTIC_RATE_PER_HOUR = 1.2
MAX_REALISTIC_RATE_PER_HOUR = 10.0
MIN_NEXT_DELTA = 0.15
MAX_NEXT_DELTA = 3.0

MODEL_DIR = None

model_candidates = [
    SCRIPT_DIR / "models_xgboost_final",
    SCRIPT_DIR.parent / "models_xgboost_final",
    SCRIPT_DIR.parent.parent / "models_xgboost_final",
    SCRIPT_DIR.parent.parent / "ml" / "models_xgboost_final",
    SCRIPT_DIR.parent.parent.parent / "ml" / "models_xgboost_final",
]

for candidate in model_candidates:
    if candidate.exists():
        MODEL_DIR = candidate
        break

if MODEL_DIR is None:
    raise FileNotFoundError("models_xgboost_final folder not found.")

MODEL_FILE = MODEL_DIR / "xgboost_dual_target.pkl"
SCALER_FILE = MODEL_DIR / "scaler_xgboost.pkl"

if not MODEL_FILE.exists():
    raise FileNotFoundError(f"Model not found: {MODEL_FILE}")

if not SCALER_FILE.exists():
    raise FileNotFoundError(f"Scaler not found: {SCALER_FILE}")


# =========================================================
# LOAD MODEL + DB
# =========================================================
model = joblib.load(MODEL_FILE)
scaler = joblib.load(SCALER_FILE)
engine = create_engine(DB_URL)


# =========================================================
# DB HELPERS
# =========================================================
def get_table_columns(table_name: str) -> set:
    sql = text("""
        SELECT column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = :table_name
    """)

    with engine.connect() as conn:
        rows = conn.execute(sql, {"table_name": table_name}).fetchall()

    return {row[0] for row in rows}


bins_columns = get_table_columns("bins")

zone_select = "b.zone_id" if "zone_id" in bins_columns else "NULL AS zone_id"
waste_select = "b.waste_type" if "waste_type" in bins_columns else "NULL AS waste_type"


# =========================================================
# FEATURES — SAME ORDER AS TRAINING
# =========================================================
features = [
    "fill_level",
    "fill_rate",
    "hours_from_cycle_start",

    "fill_lag_1",
    "fill_lag_6",
    "fill_lag_24",

    "fill_trend_1h",
    "fill_trend_6h",
    "fill_trend_24h",

    "fill_roll_mean_6h",
    "fill_roll_mean_24h",
    "fill_roll_std_24h",

    "fill_lag_168",
    "fill_trend_168h",
    "fill_roll_mean_168h",
    "fill_roll_std_168h",
    "fill_roll_max_168h",
    "fill_roll_min_168h",

    "battery_level",
    "rssi",
    "weight_kg",
    "hours_since_collection",

    "hour_sin",
    "hour_cos",
    "dow_sin",
    "dow_cos",
    "month_sin",
    "month_cos",
    "is_weekend",

    "temp",
    "rhum",
    "prcp",
    "wspd",
    "pres",
    "coco",

    "zone_enc",
    "type_bin_enc",
    "activity_enc",
    "density_hab_km2",
    "commerce_count",
]


# =========================================================
# GET TARGET TELEMETRY
# =========================================================
target_query = text("""
SELECT
    bt.id AS telemetry_id,
    bt.bin_id,
    bt.timestamp
FROM bin_telemetry bt
WHERE bt.id = :telemetry_id
""")

with engine.connect() as conn:
    target = pd.read_sql(
        target_query,
        conn,
        params={"telemetry_id": TELEMETRY_ID},
    )

if target.empty:
    raise ValueError(f"Telemetry not found: {TELEMETRY_ID}")

BIN_ID = int(target.iloc[0]["bin_id"])
TARGET_TIMESTAMP = target.iloc[0]["timestamp"]


# =========================================================
# READ HISTORY FOR SAME BIN
# =========================================================
history_sql = f"""
SELECT
    bt.id AS telemetry_id,
    bt.bin_id,
    bt.timestamp,
    bt.fill_level,
    bt.weight_kg,
    bt.battery_level,
    bt.rssi,
    bt.collected,
    bt.status,
    bt.source,

    {zone_select},
    {waste_select}

FROM bin_telemetry bt
LEFT JOIN bins b ON b.id = bt.bin_id
WHERE bt.bin_id = :bin_id
  AND bt.timestamp <= :target_timestamp
ORDER BY bt.timestamp
"""

history_query = text(history_sql)

with engine.connect() as conn:
    df = pd.read_sql(
        history_query,
        conn,
        params={
            "bin_id": BIN_ID,
            "target_timestamp": TARGET_TIMESTAMP,
        },
    )

if df.empty:
    raise ValueError(f"No history found for bin_id={BIN_ID}")


# =========================================================
# BASIC CLEANING
# =========================================================
df["timestamp"] = pd.to_datetime(df["timestamp"], errors="coerce")
df = df.dropna(subset=["timestamp"])

df = df.sort_values(["bin_id", "timestamp"]).reset_index(drop=True)

df["fill_level"] = pd.to_numeric(df["fill_level"], errors="coerce")
df = df.dropna(subset=["fill_level"])

df = df[(df["fill_level"] >= 0) & (df["fill_level"] <= 100)].copy()

if df.empty:
    raise ValueError("No valid fill_level rows after cleaning.")


# =========================================================
# RAW RATE FROM REAL 5-MIN TELEMETRY
# =========================================================
raw_df = df.copy()

raw_df["collected"] = (
    raw_df["collected"]
    .astype(str)
    .str.lower()
    .map({
        "true": 1,
        "false": 0,
        "1": 1,
        "0": 0,
        "t": 1,
        "f": 0,
        "none": 0,
        "nan": 0,
    })
    .fillna(0)
    .astype(int)
)

raw_df["fill_diff"] = raw_df.groupby("bin_id")["fill_level"].diff()
raw_df["reset_detected"] = (raw_df["fill_diff"] < -20).astype(int)

raw_df["cycle_break"] = (
    (raw_df["reset_detected"] == 1) |
    (raw_df["collected"] == 1)
).astype(int)

raw_df["cycle_id"] = raw_df.groupby("bin_id")["cycle_break"].cumsum()

target_raw = raw_df[raw_df["telemetry_id"] == TELEMETRY_ID].copy()

if target_raw.empty:
    raise ValueError(f"Target telemetry {TELEMETRY_ID} not found in raw history.")

target_cycle_id = target_raw.iloc[0]["cycle_id"]

recent_raw = raw_df[
    (raw_df["cycle_id"] == target_cycle_id) &
    (raw_df["telemetry_id"] <= TELEMETRY_ID) &
    (raw_df["collected"] == 0) &
    (raw_df["reset_detected"] == 0)
].sort_values("timestamp").tail(12).copy()

raw_rate_per_hour = 0.0

if len(recent_raw) >= 2:
    first = recent_raw.iloc[0]
    last = recent_raw.iloc[-1]

    hours_diff = (
        pd.to_datetime(last["timestamp"]) -
        pd.to_datetime(first["timestamp"])
    ).total_seconds() / 3600.0

    fill_diff = float(last["fill_level"]) - float(first["fill_level"])

    if hours_diff > 0 and fill_diff > 0:
        raw_rate_per_hour = fill_diff / hours_diff


# =========================================================
# RESAMPLE TO HOURLY HISTORY FOR XGBOOST FEATURES
# =========================================================
target_original = df[df["telemetry_id"] == TELEMETRY_ID].copy()

if target_original.empty:
    raise ValueError(f"Target telemetry {TELEMETRY_ID} not found before resampling.")

target_row = target_original.iloc[0].copy()

df = df.set_index("timestamp").sort_index()

hourly_df = (
    df
    .groupby("bin_id")
    .resample("1h")
    .last()
    .drop(columns=["bin_id"], errors="ignore")
    .reset_index()
)

target_row_df = pd.DataFrame([target_row])
target_row_df["timestamp"] = pd.to_datetime(target_row_df["timestamp"], errors="coerce")

df = pd.concat([hourly_df, target_row_df], ignore_index=True)

df = df.dropna(subset=["timestamp", "fill_level"])
df = df.sort_values(["bin_id", "timestamp", "telemetry_id"]).reset_index(drop=True)
df = df.drop_duplicates(subset=["telemetry_id"], keep="last")


# =========================================================
# CLEAN COLLECTED AFTER RESAMPLING
# =========================================================
df["collected"] = (
    df["collected"]
    .astype(str)
    .str.lower()
    .map({
        "true": 1,
        "false": 0,
        "1": 1,
        "0": 0,
        "t": 1,
        "f": 0,
        "none": 0,
        "nan": 0,
    })
    .fillna(0)
    .astype(int)
)


# =========================================================
# CYCLE DETECTION ON HOURLY DATA
# =========================================================
df["fill_diff"] = df.groupby("bin_id")["fill_level"].diff()
df["reset_detected"] = (df["fill_diff"] < -20).astype(int)

df["cycle_break"] = (
    (df["reset_detected"] == 1) |
    (df["collected"] == 1)
).astype(int)

df["cycle_id"] = df.groupby("bin_id")["cycle_break"].cumsum()

df["hours_from_cycle_start"] = (
    df.groupby(["bin_id", "cycle_id"]).cumcount()
)

df = df[
    (
        (df["collected"] == 0) &
        (df["reset_detected"] == 0)
    )
    |
    (df["telemetry_id"] == TELEMETRY_ID)
].copy()

if df.empty:
    raise ValueError("No valid telemetry rows after cycle filtering.")


# =========================================================
# TIME FEATURES
# =========================================================
df["hour"] = df["timestamp"].dt.hour
df["day_of_week"] = df["timestamp"].dt.dayofweek
df["month"] = df["timestamp"].dt.month

df["is_weekend"] = (df["day_of_week"] >= 5).astype(int)

df["hour_sin"] = np.sin(2 * np.pi * df["hour"] / 24)
df["hour_cos"] = np.cos(2 * np.pi * df["hour"] / 24)

df["dow_sin"] = np.sin(2 * np.pi * df["day_of_week"] / 7)
df["dow_cos"] = np.cos(2 * np.pi * df["day_of_week"] / 7)

df["month_sin"] = np.sin(2 * np.pi * df["month"] / 12)
df["month_cos"] = np.cos(2 * np.pi * df["month"] / 12)


# =========================================================
# ENCODING
# =========================================================
df["zone_enc"] = (
    pd.to_numeric(df["zone_id"], errors="coerce")
    .fillna(0)
    .astype(int)
)

waste_type_mapping = {
    "GRAY": 5,
    "GREEN": 2,
    "YELLOW": 1,
    "WHITE": 3,
    "ORGANIC": 0,
    "PLASTIC": 1,
    "GLASS": 2,
    "PAPER": 3,
    "METAL": 4,
    "MIXED": 5,
    "GENERAL": 5,
    "ORDINARY": 5,
    "OTHER": 5,
}

df["type_bin_enc"] = (
    df["waste_type"]
    .astype(str)
    .str.upper()
    .map(waste_type_mapping)
    .fillna(5)
    .astype(int)
)

df["activity_enc"] = 1


# =========================================================
# FEATURE ENGINEERING
# =========================================================
df["fill_rate"] = (
    df.groupby(["bin_id", "cycle_id"])["fill_level"]
    .diff()
    .clip(lower=0)
    .fillna(0)
)

df["fill_lag_1"] = df.groupby(["bin_id", "cycle_id"])["fill_level"].shift(1)
df["fill_lag_6"] = df.groupby(["bin_id", "cycle_id"])["fill_level"].shift(6)
df["fill_lag_24"] = df.groupby(["bin_id", "cycle_id"])["fill_level"].shift(24)

df["fill_trend_1h"] = df["fill_level"] - df["fill_lag_1"]
df["fill_trend_6h"] = df["fill_level"] - df["fill_lag_6"]
df["fill_trend_24h"] = df["fill_level"] - df["fill_lag_24"]

df["fill_roll_mean_6h"] = (
    df.groupby(["bin_id", "cycle_id"])["fill_level"]
    .transform(lambda s: s.shift(1).rolling(6, min_periods=1).mean())
)

df["fill_roll_mean_24h"] = (
    df.groupby(["bin_id", "cycle_id"])["fill_level"]
    .transform(lambda s: s.shift(1).rolling(24, min_periods=1).mean())
)

df["fill_roll_std_24h"] = (
    df.groupby(["bin_id", "cycle_id"])["fill_level"]
    .transform(lambda s: s.shift(1).rolling(24, min_periods=2).std())
)

df["fill_lag_168"] = df.groupby(["bin_id", "cycle_id"])["fill_level"].shift(168)
df["fill_trend_168h"] = df["fill_level"] - df["fill_lag_168"]

df["fill_roll_mean_168h"] = (
    df.groupby(["bin_id", "cycle_id"])["fill_level"]
    .transform(lambda s: s.shift(1).rolling(168, min_periods=24).mean())
)

df["fill_roll_std_168h"] = (
    df.groupby(["bin_id", "cycle_id"])["fill_level"]
    .transform(lambda s: s.shift(1).rolling(168, min_periods=24).std())
)

df["fill_roll_max_168h"] = (
    df.groupby(["bin_id", "cycle_id"])["fill_level"]
    .transform(lambda s: s.shift(1).rolling(168, min_periods=24).max())
)

df["fill_roll_min_168h"] = (
    df.groupby(["bin_id", "cycle_id"])["fill_level"]
    .transform(lambda s: s.shift(1).rolling(168, min_periods=24).min())
)


# =========================================================
# DEFAULT VALUES
# =========================================================
defaults = {
    "battery_level": 80,
    "rssi": -70,
    "weight_kg": df["fill_level"] * 0.09,
    "hours_since_collection": 0,

    "temp": 15,
    "rhum": 70,
    "prcp": 0,
    "wspd": 10,
    "pres": 1015,
    "coco": 1,

    "density_hab_km2": 28000,
    "commerce_count": 25,
}

for col, val in defaults.items():
    if col not in df.columns:
        df[col] = val

    df[col] = df[col].fillna(val)


# =========================================================
# TARGET ROW
# =========================================================
target_df = df[df["telemetry_id"] == TELEMETRY_ID].copy()

if target_df.empty:
    raise ValueError(f"Target telemetry {TELEMETRY_ID} was removed after filtering.")

target_df[features] = (
    target_df[features]
    .replace([np.inf, -np.inf], np.nan)
    .fillna(0)
)


# =========================================================
# XGBOOST PREDICT — FILL NEXT ONLY
# =========================================================
X = target_df[features]
X_scaled = scaler.transform(X)

pred = model.predict(X_scaled)

model_fill_next = float(np.clip(pred[0, 0], 0, 100))

current_fill = float(target_df.iloc[0]["fill_level"])

model_fill_next = max(model_fill_next, current_fill)
model_fill_next = float(np.clip(model_fill_next, 0, 100))

model_delta = max(0.0, model_fill_next - current_fill)
model_rate_per_hour = model_delta / TELEMETRY_INTERVAL_HOURS if model_delta > 0 else 0.0


# =========================================================
# STABLE RATE FOR DEMO / REALISTIC TIME
# =========================================================
if raw_rate_per_hour >= MIN_REALISTIC_RATE_PER_HOUR:
    estimated_rate_per_hour = raw_rate_per_hour

elif model_rate_per_hour >= MIN_REALISTIC_RATE_PER_HOUR:
    estimated_rate_per_hour = model_rate_per_hour

else:
    if current_fill < 30:
        estimated_rate_per_hour = 2.5
    elif current_fill < 50:
        estimated_rate_per_hour = 3.0
    elif current_fill < 70:
        estimated_rate_per_hour = 3.8
    else:
        estimated_rate_per_hour = 5.0

estimated_rate_per_hour = float(
    np.clip(
        estimated_rate_per_hour,
        MIN_REALISTIC_RATE_PER_HOUR,
        MAX_REALISTIC_RATE_PER_HOUR,
    )
)


# =========================================================
# LOGICAL NEXT FILL CORRECTION
# =========================================================
expected_delta = estimated_rate_per_hour * TELEMETRY_INTERVAL_HOURS

if current_fill >= 100:
    predicted_fill_next = 100.0
elif current_fill >= TARGET_FILL_LEVEL:
    predicted_fill_next = max(current_fill, model_fill_next)
else:
    corrected_delta = float(np.clip(expected_delta, MIN_NEXT_DELTA, MAX_NEXT_DELTA))
    max_allowed_next = min(100.0, current_fill + MAX_NEXT_DELTA)
    natural_next = min(100.0, current_fill + corrected_delta)

    if model_fill_next < natural_next:
        predicted_fill_next = natural_next
    else:
        predicted_fill_next = min(model_fill_next, max_allowed_next)

predicted_fill_next = float(np.clip(max(predicted_fill_next, current_fill), 0, 100))


# =========================================================
# FINAL TIME_TO_FULL LOGIC
# =========================================================
remaining_fill = max(0.0, TARGET_FILL_LEVEL - current_fill)


if current_fill >= TARGET_FILL_LEVEL:
    predicted_hours = 0.0
else:
    predicted_hours = remaining_fill / estimated_rate_per_hour


if current_fill >= TARGET_FILL_LEVEL:
    predicted_hours = 0.0
elif current_fill >= 85:
    predicted_hours = min(predicted_hours, 2.0)
elif current_fill >= 80:
    predicted_hours = min(predicted_hours, 3.0)
elif current_fill >= 70:
    predicted_hours = min(predicted_hours, 5.0)

predicted_hours = float(np.clip(predicted_hours, 0, TIME_MAX))


# =========================================================
# DECISION LOGIC
# =========================================================
def compute_alert_status(hours):
    if hours <= 2:
        return "HIGH"

    if hours <= 5:
        return "MEDIUM"

    return "LOW"


def compute_priority_score(fill, hours):
    fill_score = fill / 100.0
    time_score = 1.0 - min(hours, TIME_MAX) / TIME_MAX

    score = (0.65 * fill_score) + (0.35 * time_score)

    return round(float(score), 2)


alert_status = compute_alert_status(predicted_hours)
priority_score = compute_priority_score(predicted_fill_next, predicted_hours)

should_collect = bool(
    predicted_hours <= 2
    or predicted_fill_next >= 90
)


# =========================================================
# DB SAVE — DELETE OLD PREDICTION FOR SAME TELEMETRY THEN INSERT
# =========================================================
delete_old_fill_sql = text("""
DELETE FROM bin_predictions
WHERE telemetry_id = :telemetry_id
""")

delete_old_time_sql = text("""
DELETE FROM bin_time_predictions
WHERE telemetry_id = :telemetry_id
""")

insert_fill_sql = text("""
INSERT INTO bin_predictions (
    bin_id,
    telemetry_id,
    predicted_fill_next,
    alert_status,
    priority_score,
    should_collect,
    created_at
)
VALUES (
    :bin_id,
    :telemetry_id,
    :predicted_fill_next,
    :alert_status,
    :priority_score,
    :should_collect,
    NOW()
)
""")

insert_time_sql = text("""
INSERT INTO bin_time_predictions (
    bin_id,
    telemetry_id,
    predicted_hours,
    alert_status,
    priority_score,
    should_collect,
    created_at
)
VALUES (
    :bin_id,
    :telemetry_id,
    :predicted_hours,
    :alert_status,
    :priority_score,
    :should_collect,
    NOW()
)
""")

fill_row = {
    "bin_id": BIN_ID,
    "telemetry_id": TELEMETRY_ID,
    "predicted_fill_next": round(predicted_fill_next, 2),
    "alert_status": alert_status,
    "priority_score": priority_score,
    "should_collect": should_collect,
}

time_row = {
    "bin_id": BIN_ID,
    "telemetry_id": TELEMETRY_ID,
    "predicted_hours": round(predicted_hours, 2),
    "alert_status": alert_status,
    "priority_score": priority_score,
    "should_collect": should_collect,
}

with engine.begin() as conn:
    conn.execute(delete_old_fill_sql, {"telemetry_id": TELEMETRY_ID})
    conn.execute(delete_old_time_sql, {"telemetry_id": TELEMETRY_ID})

    conn.execute(insert_fill_sql, fill_row)
    conn.execute(insert_time_sql, time_row)


# =========================================================
# FINAL OUTPUT FOR JAVA LOGS
# =========================================================
print(
    f"{round(predicted_fill_next, 2)},"
    f"{round(predicted_hours, 2)},"
    f"{alert_status},"
    f"{priority_score},"
    f"{str(should_collect).lower()}"
)
