# lstm_final_strict_time_to_90.py

import os
import json
import joblib
import numpy as np
import pandas as pd
import tensorflow as tf
import matplotlib.pyplot as plt

from datetime import datetime
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score

from tensorflow.keras.models import Model
from tensorflow.keras.layers import Input, LSTM, Dense, Dropout, LayerNormalization
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau, ModelCheckpoint
from tensorflow.keras.regularizers import l2


# =========================================================
# CONFIG
# =========================================================
print("TensorFlow:", tf.__version__)
print("GPU:", tf.config.list_physical_devices("GPU"))

RANDOM_SEED = 42
np.random.seed(RANDOM_SEED)
tf.random.set_seed(RANDOM_SEED)

# بدّل الاسم هنا كان dataset عندك اسمها آخر
DATA_FILE = "final_dataset_paris15_v4_fixed.csv"
# DATA_FILE = "final_dataset_paris15_v5_realistic.csv"
# DATA_FILE = "final_dataset_paris15_v6_realistic.csv"

MODEL_DIR = "models_lstm_final"
os.makedirs(MODEL_DIR, exist_ok=True)

MODEL_FILE = f"{MODEL_DIR}/lstm_final_time_to_90.keras"
BEST_WEIGHTS_FILE = f"{MODEL_DIR}/lstm_final_time_to_90_best.weights.h5"
SCALER_X_FILE = f"{MODEL_DIR}/scaler_X.pkl"
SCALER_Y_FILE = f"{MODEL_DIR}/scaler_y.pkl"
FEATURES_FILE = f"{MODEL_DIR}/features.pkl"
METRICS_CSV_FILE = f"{MODEL_DIR}/metrics_lstm_strict.csv"
METRICS_JSON_FILE = f"{MODEL_DIR}/metrics_lstm_strict.json"
COMPARISON_FILE = f"{MODEL_DIR}/comparison_lstm_final.csv"

SEQ_LEN = 24

# نخدمو على seuil critique 90%, موش 100%
FULL_THRESHOLD = 90.0

# horizon opérationnel: فقط الحالات اللي توصل 90% في 36 ساعة
TIME_MAX = 36.0

TEST_RATIO = 0.20
VAL_RATIO = 0.15

EPOCHS = 80
BATCH_SIZE = 32

TARGET_FILL = "fill_level_next"
TARGET_TIME = "time_to_90_real"
TARGET_TIME_LOG = "time_to_90_log"


# =========================================================
# LOAD DATA
# =========================================================
df = pd.read_csv(DATA_FILE)
df.columns = df.columns.str.strip()

if "time" in df.columns:
    df = df.rename(columns={"time": "timestamp"})
elif "timestamp" not in df.columns:
    raise ValueError("Colonne temps manquante: time ou timestamp")

df["timestamp"] = pd.to_datetime(df["timestamp"], errors="coerce")
df = df.dropna(subset=["timestamp"])

df = df.sort_values(["bin_id", "timestamp"]).reset_index(drop=True)

df = df.dropna(subset=["bin_id", "fill_level"]).copy()
df = df[(df["fill_level"] >= 0) & (df["fill_level"] <= 100)].copy()


# =========================================================
# BASIC CLEANING
# =========================================================
if "anomaly_type" in df.columns:
    df["has_anomaly"] = df["anomaly_type"].astype(str).str.lower().ne("none").astype(int)
else:
    df["has_anomaly"] = 0

if "collected" in df.columns:
    df["collected"] = (
        df["collected"]
        .astype(str)
        .str.lower()
        .map({
            "true": 1, "t": 1, "1": 1,
            "false": 0, "f": 0, "0": 0
        })
        .fillna(0)
        .astype(int)
    )
else:
    df["collected"] = 0

# نحيو anomalies
df = df[df["has_anomaly"] == 0].copy()


# =========================================================
# CYCLE ID
# يمنع خلط قبل/بعد collecte
# =========================================================
df["fill_diff"] = df.groupby("bin_id")["fill_level"].diff()
df["reset_detected"] = (df["fill_diff"] < -20).astype(int)

df["cycle_break"] = ((df["reset_detected"] == 1) | (df["collected"] == 1)).astype(int)
df["cycle_id"] = df.groupby("bin_id")["cycle_break"].cumsum()

df["hours_from_cycle_start"] = df.groupby(["bin_id", "cycle_id"]).cumcount()

# نحيو lignes متاع collecte/reset
df = df[
    (df["collected"] == 0) &
    (df["reset_detected"] == 0)
].copy()

# أول ساعات بعد collecte تكون unstable
df = df[df["hours_from_cycle_start"] >= 6].copy()


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
# ENCODING SIMPLE
# =========================================================
df["zone_enc"] = df["zone"].astype("category").cat.codes if "zone" in df.columns else 0
df["type_bin_enc"] = df["type_bin"].astype("category").cat.codes if "type_bin" in df.columns else 0

if "activity_level" in df.columns:
    df["activity_enc"] = (
        df["activity_level"]
        .map({"Low": 0, "Medium": 1, "High": 2})
        .fillna(1)
        .astype(int)
    )
else:
    df["activity_enc"] = 1


# =========================================================
# FILL RATE INSIDE CYCLE
# =========================================================
df["fill_rate"] = (
    df.groupby(["bin_id", "cycle_id"])["fill_level"]
    .diff()
    .clip(lower=0)
    .fillna(0)
)

df["fill_rate_mean_3h"] = df.groupby(["bin_id", "cycle_id"])["fill_rate"].transform(
    lambda s: s.shift(1).rolling(3, min_periods=1).mean()
)

df["fill_rate_mean_6h"] = df.groupby(["bin_id", "cycle_id"])["fill_rate"].transform(
    lambda s: s.shift(1).rolling(6, min_periods=1).mean()
)

df["fill_rate_mean_12h"] = df.groupby(["bin_id", "cycle_id"])["fill_rate"].transform(
    lambda s: s.shift(1).rolling(12, min_periods=2).mean()
)

df["fill_rate_mean_24h"] = df.groupby(["bin_id", "cycle_id"])["fill_rate"].transform(
    lambda s: s.shift(1).rolling(24, min_periods=1).mean()
)


# =========================================================
# LAGS / ROLLING FEATURES
# =========================================================
df["fill_lag_1"] = df.groupby(["bin_id", "cycle_id"])["fill_level"].shift(1)
df["fill_lag_6"] = df.groupby(["bin_id", "cycle_id"])["fill_level"].shift(6)
df["fill_lag_24"] = df.groupby(["bin_id", "cycle_id"])["fill_level"].shift(24)

df["fill_trend_1h"] = df["fill_level"] - df["fill_lag_1"]
df["fill_trend_6h"] = df["fill_level"] - df["fill_lag_6"]
df["fill_trend_24h"] = df["fill_level"] - df["fill_lag_24"]

df["fill_roll_mean_6h"] = df.groupby(["bin_id", "cycle_id"])["fill_level"].transform(
    lambda s: s.shift(1).rolling(6, min_periods=1).mean()
)

df["fill_roll_mean_24h"] = df.groupby(["bin_id", "cycle_id"])["fill_level"].transform(
    lambda s: s.shift(1).rolling(24, min_periods=1).mean()
)

df["fill_roll_std_24h"] = df.groupby(["bin_id", "cycle_id"])["fill_level"].transform(
    lambda s: s.shift(1).rolling(24, min_periods=2).std()
)

df["remaining_capacity"] = (FULL_THRESHOLD - df["fill_level"]).clip(lower=0)

# Rule features فقط input مساعد، موش target
df["hours_to_90_rule_3h"] = (
    df["remaining_capacity"] / (df["fill_rate_mean_3h"] + 0.01)
).clip(0, TIME_MAX)

df["hours_to_90_rule_6h"] = (
    df["remaining_capacity"] / (df["fill_rate_mean_6h"] + 0.01)
).clip(0, TIME_MAX)

df["hours_to_90_rule_12h"] = (
    df["remaining_capacity"] / (df["fill_rate_mean_12h"] + 0.01)
).clip(0, TIME_MAX)


# =========================================================
# TARGETS
# =========================================================
df[TARGET_FILL] = df.groupby(["bin_id", "cycle_id"])["fill_level"].shift(-1)


def compute_time_to_threshold(group, threshold=90.0):
    """
    يحسب time_to_90 من المستقبل الحقيقي داخل نفس cycle.
    يعني: بعد قداش ساعة fill_level يوصل threshold.
    """
    group = group.sort_values("timestamp").reset_index(drop=True)
    values = group["fill_level"].values
    result = []

    for i in range(len(values)):
        if values[i] >= threshold:
            result.append(0.0)
            continue

        future = values[i + 1:]
        hit = np.where(future >= threshold)[0]

        if len(hit) > 0:
            result.append(float(hit[0] + 1))
        else:
            result.append(np.nan)

    group[TARGET_TIME] = result
    return group


df = df.groupby(["bin_id", "cycle_id"], group_keys=False).apply(
    compute_time_to_threshold,
    threshold=FULL_THRESHOLD
)

df = df.dropna(subset=[TARGET_FILL, TARGET_TIME]).copy()

# نحيو jumps غريبة
df["target_jump"] = (df[TARGET_FILL] - df["fill_level"]).abs()
df = df[df["target_jump"] <= 8].copy()

# نركزو على horizon utile opérationnel
df = df[(df[TARGET_TIME] >= 0) & (df[TARGET_TIME] <= TIME_MAX)].copy()

df[TARGET_FILL] = df[TARGET_FILL].clip(0, 100)

df[TARGET_TIME_LOG] = np.log1p(df[TARGET_TIME])
TIME_LOG_MAX = np.log1p(TIME_MAX)


# =========================================================
# DEFAULTS
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

    "fill_lag_1": df["fill_level"],
    "fill_lag_6": df["fill_level"],
    "fill_lag_24": df["fill_level"],

    "fill_trend_1h": 0,
    "fill_trend_6h": 0,
    "fill_trend_24h": 0,

    "fill_roll_mean_6h": df["fill_level"],
    "fill_roll_mean_24h": df["fill_level"],
    "fill_roll_std_24h": 0,

    "fill_rate_mean_3h": 0,
    "fill_rate_mean_6h": 0,
    "fill_rate_mean_12h": 0,
    "fill_rate_mean_24h": 0,

    "remaining_capacity": 0,
    "hours_to_90_rule_3h": TIME_MAX,
    "hours_to_90_rule_6h": TIME_MAX,
    "hours_to_90_rule_12h": TIME_MAX,
}

for col, val in defaults.items():
    if col not in df.columns:
        df[col] = val
    df[col] = df[col].fillna(val)


# =========================================================
# FEATURES
# =========================================================
features = [
    "fill_level",
    "fill_rate",
    "remaining_capacity",
    "hours_from_cycle_start",

    "hours_to_90_rule_3h",
    "hours_to_90_rule_6h",
    "hours_to_90_rule_12h",

    "fill_lag_1",
    "fill_lag_6",
    "fill_lag_24",

    "fill_trend_1h",
    "fill_trend_6h",
    "fill_trend_24h",

    "fill_roll_mean_6h",
    "fill_roll_mean_24h",
    "fill_roll_std_24h",

    "fill_rate_mean_3h",
    "fill_rate_mean_6h",
    "fill_rate_mean_12h",
    "fill_rate_mean_24h",

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

df[features] = df[features].replace([np.inf, -np.inf], np.nan).fillna(0)


# =========================================================
# CREATE SEQUENCES
# =========================================================
def create_sequences(data, feature_cols, seq_len):
    X_list = []
    y_fill_list = []
    y_time_list = []
    time_list = []
    bin_list = []
    cycle_list = []

    for (bin_id, cycle_id), group in data.groupby(["bin_id", "cycle_id"]):
        group = group.sort_values("timestamp").reset_index(drop=True)

        if len(group) <= seq_len:
            continue

        X_values = group[feature_cols].values.astype(np.float32)
        y_fill_values = group[TARGET_FILL].values.astype(np.float32)
        y_time_values = group[TARGET_TIME_LOG].values.astype(np.float32)
        times = group["timestamp"].values

        for i in range(seq_len - 1, len(group) - 1):
            window = group.iloc[i - seq_len + 1:i + 1]

            if window["target_jump"].max() > 8:
                continue

            X_list.append(X_values[i - seq_len + 1:i + 1])
            y_fill_list.append(y_fill_values[i])
            y_time_list.append(y_time_values[i])
            time_list.append(times[i])
            bin_list.append(bin_id)
            cycle_list.append(cycle_id)

    return (
        np.array(X_list, dtype=np.float32),
        np.array(y_fill_list, dtype=np.float32),
        np.array(y_time_list, dtype=np.float32),
        np.array(time_list),
        np.array(bin_list),
        np.array(cycle_list),
    )


X, y_fill, y_time_log, times, bin_ids, cycle_ids = create_sequences(
    df,
    features,
    SEQ_LEN
)

print("Rows used:", len(df))
print("X:", X.shape)
print("y_fill:", y_fill.shape)
print("y_time:", y_time_log.shape)

if len(X) == 0:
    raise ValueError("No sequences created. Na99es SEQ_LEN ou check filters.")


# =========================================================
# GLOBAL TEMPORAL SPLIT
# =========================================================
order = np.argsort(times)

X = X[order]
y_fill = y_fill[order]
y_time_log = y_time_log[order]
times = times[order]
bin_ids = bin_ids[order]
cycle_ids = cycle_ids[order]

split_idx = int(len(X) * (1 - TEST_RATIO))

X_train_all = X[:split_idx]
X_test = X[split_idx:]

y_fill_train_all = y_fill[:split_idx]
y_fill_test = y_fill[split_idx:]

y_time_train_all = y_time_log[:split_idx]
y_time_test = y_time_log[split_idx:]

times_test = times[split_idx:]
bin_ids_test = bin_ids[split_idx:]
cycle_ids_test = cycle_ids[split_idx:]

val_idx = int(len(X_train_all) * (1 - VAL_RATIO))

X_train = X_train_all[:val_idx]
X_val = X_train_all[val_idx:]

y_fill_train = y_fill_train_all[:val_idx]
y_fill_val = y_fill_train_all[val_idx:]

y_time_train = y_time_train_all[:val_idx]
y_time_val = y_time_train_all[val_idx:]

print("Train:", X_train.shape)
print("Val:", X_val.shape)
print("Test:", X_test.shape)


# =========================================================
# SCALING AFTER SPLIT
# =========================================================
scaler_X = StandardScaler()
scaler_X.fit(X_train.reshape(-1, X_train.shape[-1]))


def scale_X(values):
    shape = values.shape
    out = scaler_X.transform(values.reshape(-1, shape[-1]))
    return out.reshape(shape).astype(np.float32)


def scale_fill(values):
    return (values.reshape(-1, 1) / 100.0).astype(np.float32)


def unscale_fill(values):
    return (values.reshape(-1) * 100.0).astype(np.float32)


def scale_time_log(values):
    return (values.reshape(-1, 1) / TIME_LOG_MAX).astype(np.float32)


def unscale_time_log(values):
    return np.expm1(values.reshape(-1) * TIME_LOG_MAX).astype(np.float32)


X_train_s = scale_X(X_train)
X_val_s = scale_X(X_val)
X_test_s = scale_X(X_test)

y_fill_train_s = scale_fill(y_fill_train)
y_fill_val_s = scale_fill(y_fill_val)
y_fill_test_s = scale_fill(y_fill_test)

y_time_train_s = scale_time_log(y_time_train)
y_time_val_s = scale_time_log(y_time_val)
y_time_test_s = scale_time_log(y_time_test)


# =========================================================
# SAMPLE WEIGHTS
# نعطيو أهمية أكثر للحالات القريبة من 90%
# =========================================================
real_time_train = np.expm1(y_time_train)
real_time_val = np.expm1(y_time_val)

w_fill_train = np.ones_like(y_fill_train, dtype=np.float32)
w_fill_val = np.ones_like(y_fill_val, dtype=np.float32)

w_time_train = np.ones_like(real_time_train, dtype=np.float32)
w_time_val = np.ones_like(real_time_val, dtype=np.float32)

w_time_train += (real_time_train <= 24).astype(np.float32) * 4.0
w_time_train += (real_time_train <= 12).astype(np.float32) * 6.0
w_time_train += (real_time_train <= 6).astype(np.float32) * 12.0
w_time_train += (real_time_train <= 3).astype(np.float32) * 18.0

w_time_val += (real_time_val <= 24).astype(np.float32) * 4.0
w_time_val += (real_time_val <= 12).astype(np.float32) * 6.0
w_time_val += (real_time_val <= 6).astype(np.float32) * 12.0
w_time_val += (real_time_val <= 3).astype(np.float32) * 18.0


# =========================================================
# LSTM MODEL
# =========================================================
inputs = Input(shape=(SEQ_LEN, len(features)))

x = LSTM(
    64,
    return_sequences=True,
    recurrent_dropout=0.10
)(inputs)
x = LayerNormalization()(x)
x = Dropout(0.20)(x)

x = LSTM(
    32,
    return_sequences=False,
    recurrent_dropout=0.10
)(x)
x = LayerNormalization()(x)
x = Dropout(0.20)(x)

shared = Dense(
    32,
    activation="relu",
    kernel_regularizer=l2(1e-4)
)(x)
shared = Dropout(0.10)(shared)

fill_branch = Dense(
    24,
    activation="relu",
    kernel_regularizer=l2(1e-4)
)(shared)
fill_output = Dense(1, activation="sigmoid", name="fill_output")(fill_branch)

time_branch = Dense(
    48,
    activation="relu",
    kernel_regularizer=l2(1e-4)
)(shared)
time_branch = Dropout(0.10)(time_branch)
time_output = Dense(1, activation="sigmoid", name="time_output")(time_branch)

model = Model(
    inputs=inputs,
    outputs={
        "fill_output": fill_output,
        "time_output": time_output,
    }
)

model.compile(
    optimizer=tf.keras.optimizers.Adam(learning_rate=0.0002, clipnorm=0.5),
    loss={
        "fill_output": tf.keras.losses.Huber(delta=0.03),
        "time_output": "mae",
    },
    loss_weights={
        "fill_output": 1.0,
        "time_output": 5.0,
    },
    metrics={
        "fill_output": ["mae"],
        "time_output": ["mae"],
    },
)

model.summary()


# =========================================================
# CALLBACKS
# =========================================================
callbacks = [
    EarlyStopping(
        monitor="val_time_output_loss",
        patience=14,
        restore_best_weights=True,
        mode="min",
    ),
    ReduceLROnPlateau(
        monitor="val_time_output_loss",
        factor=0.5,
        patience=5,
        min_lr=1e-6,
        mode="min",
    ),
    ModelCheckpoint(
        BEST_WEIGHTS_FILE,
        monitor="val_time_output_loss",
        save_best_only=True,
        save_weights_only=True,
        mode="min",
    ),
]


# =========================================================
# TRAIN
# =========================================================
history = model.fit(
    X_train_s,
    {
        "fill_output": y_fill_train_s,
        "time_output": y_time_train_s,
    },
    validation_data=(
        X_val_s,
        {
            "fill_output": y_fill_val_s,
            "time_output": y_time_val_s,
        },
        {
            "fill_output": w_fill_val,
            "time_output": w_time_val,
        },
    ),
    sample_weight={
        "fill_output": w_fill_train,
        "time_output": w_time_train,
    },
    epochs=EPOCHS,
    batch_size=BATCH_SIZE,
    callbacks=callbacks,
    shuffle=False,
    verbose=1,
)

model.load_weights(BEST_WEIGHTS_FILE)


# =========================================================
# EVALUATION
# =========================================================
pred = model.predict(X_test_s, verbose=0)

fill_pred = np.clip(unscale_fill(pred["fill_output"]), 0, 100)
fill_true = np.clip(unscale_fill(y_fill_test_s), 0, 100)

time_pred = np.clip(unscale_time_log(pred["time_output"]), 0, TIME_MAX)
time_true = np.clip(unscale_time_log(y_time_test_s), 0, TIME_MAX)

err_time_h = np.abs(time_true - time_pred)
err_time_min = err_time_h * 60

fill_mae = mean_absolute_error(fill_true, fill_pred)
fill_rmse = np.sqrt(mean_squared_error(fill_true, fill_pred))
fill_r2 = r2_score(fill_true, fill_pred)

time_mae = mean_absolute_error(time_true, time_pred)
time_rmse = np.sqrt(mean_squared_error(time_true, time_pred))
time_r2 = r2_score(time_true, time_pred)

metrics_df = pd.DataFrame([
    ["fill_level_next", fill_mae, fill_rmse, fill_r2],
    ["time_to_90_hours", time_mae, time_rmse, time_r2],
    ["time_to_90_minutes", time_mae * 60, time_rmse * 60, time_r2],
], columns=["target", "MAE", "RMSE", "R2"])

print("\n===== FINAL METRICS LSTM STRICT =====")
print(metrics_df.to_string(index=False))

print("\n===== TIME QUALITY LSTM STRICT =====")
print(f"MAE time: {time_mae:.3f}h = {time_mae*60:.1f} min")
print(f"RMSE time: {time_rmse:.3f}h = {time_rmse*60:.1f} min")
print(f"% <= 30 min: {(err_time_h <= 0.5).mean()*100:.2f}%")
print(f"% <= 60 min: {(err_time_h <= 1.0).mean()*100:.2f}%")
print(f"% <= 90 min: {(err_time_h <= 1.5).mean()*100:.2f}%")
print(f"% <= 120 min: {(err_time_h <= 2.0).mean()*100:.2f}%")

metrics_df.to_csv(METRICS_CSV_FILE, index=False)


# =========================================================
# SAVE
# =========================================================
model.save(MODEL_FILE)

joblib.dump(scaler_X, SCALER_X_FILE)

joblib.dump(
    {
        "fill_scale": 100.0,
        "time_transform": "log1p",
        "time_log_max": float(TIME_LOG_MAX),
        "time_max": TIME_MAX,
        "full_threshold": FULL_THRESHOLD,
        "features": features,
        "target_fill": TARGET_FILL,
        "target_time": TARGET_TIME,
        "model_type": "Strict LSTM",
        "note": "time_to_90 is computed from future real observations inside each collection cycle."
    },
    SCALER_Y_FILE,
)

joblib.dump(features, FEATURES_FILE)

comparison = pd.DataFrame({
    "bin_id": bin_ids_test,
    "cycle_id": cycle_ids_test,
    "timestamp": times_test,

    "fill_real": fill_true,
    "fill_pred": fill_pred,
    "error_fill": np.abs(fill_true - fill_pred),

    "time_to_90_real": time_true,
    "time_to_90_pred": time_pred,
    "error_time_h": err_time_h,
    "error_time_min": err_time_min,
})

comparison.to_csv(COMPARISON_FILE, index=False)

metrics_json = {
    "trained_at": datetime.now().isoformat(),
    "model_type": "Strict LSTM",
    "seq_len": SEQ_LEN,
    "threshold": FULL_THRESHOLD,
    "time_max": TIME_MAX,
    "train_samples": int(len(X_train)),
    "val_samples": int(len(X_val)),
    "test_samples": int(len(X_test)),
    "metrics": {
        "fill_level_next": {
            "MAE": float(fill_mae),
            "RMSE": float(fill_rmse),
            "R2": float(fill_r2),
        },
        "time_to_90_hours": {
            "MAE": float(time_mae),
            "RMSE": float(time_rmse),
            "R2": float(time_r2),
            "pct_error_le_30min": float((err_time_h <= 0.5).mean()*100),
            "pct_error_le_60min": float((err_time_h <= 1.0).mean()*100),
            "pct_error_le_90min": float((err_time_h <= 1.5).mean()*100),
            "pct_error_le_120min": float((err_time_h <= 2.0).mean()*100),
        }
    }
}

with open(METRICS_JSON_FILE, "w", encoding="utf-8") as f:
    json.dump(metrics_json, f, indent=4)

print("\nSaved:")
print(MODEL_FILE)
print(SCALER_X_FILE)
print(SCALER_Y_FILE)
print(FEATURES_FILE)
print(COMPARISON_FILE)
print(METRICS_CSV_FILE)
print(METRICS_JSON_FILE)


# =========================================================
# PLOTS
# =========================================================
plt.figure(figsize=(10, 5))
plt.plot(history.history["loss"], label="Train total loss")
plt.plot(history.history["val_loss"], label="Val total loss")
plt.plot(history.history["time_output_loss"], label="Train time loss")
plt.plot(history.history["val_time_output_loss"], label="Val time loss")
plt.legend()
plt.grid(True)
plt.title("Strict LSTM Training History")
plt.savefig(f"{MODEL_DIR}/lstm_strict_loss.png", dpi=300)
plt.show()

plt.figure(figsize=(6, 6))
plt.scatter(fill_true, fill_pred, alpha=0.25, s=5)
plt.plot([0, 100], [0, 100], "--")
plt.title("Strict LSTM Scatter — Fill Level Next")
plt.xlabel("Real")
plt.ylabel("Predicted")
plt.grid(True)
plt.savefig(f"{MODEL_DIR}/lstm_strict_scatter_fill.png", dpi=300)
plt.show()

plt.figure(figsize=(6, 6))
plt.scatter(time_true, time_pred, alpha=0.25, s=5)
plt.plot([0, TIME_MAX], [0, TIME_MAX], "--")
plt.title("Strict LSTM Scatter — Time To 90%")
plt.xlabel("Real hours")
plt.ylabel("Predicted hours")
plt.grid(True)
plt.savefig(f"{MODEL_DIR}/lstm_strict_scatter_time.png", dpi=300)
plt.show()

n = min(300, len(fill_true))

plt.figure(figsize=(12, 5))
plt.plot(fill_true[:n], label="Real fill")
plt.plot(fill_pred[:n], label="Pred fill")
plt.legend()
plt.grid(True)
plt.title("Strict LSTM Fill — Real vs Predicted")
plt.savefig(f"{MODEL_DIR}/lstm_strict_curve_fill.png", dpi=300)
plt.show()

plt.figure(figsize=(12, 5))
plt.plot(time_true[:n], label="Real time")
plt.plot(time_pred[:n], label="Pred time")
plt.legend()
plt.grid(True)
plt.title("Strict LSTM Time To 90% — Real vs Predicted")
plt.savefig(f"{MODEL_DIR}/lstm_strict_curve_time.png", dpi=300)
plt.show()

plt.figure(figsize=(12, 5))
plt.plot(err_time_min[:n], label="Error time minutes")
plt.axhline(30, linestyle="--", label="30 min")
plt.axhline(60, linestyle="--", label="60 min")
plt.axhline(90, linestyle="--", label="90 min")
plt.legend()
plt.grid(True)
plt.title("Strict LSTM Time Error")
plt.savefig(f"{MODEL_DIR}/lstm_strict_error_time.png", dpi=300)
plt.show()

print("\n===== BEST TIME PREDICTIONS LSTM STRICT =====")
print(comparison.sort_values("error_time_min").head(10).to_string(index=False))

print("\n===== WORST TIME PREDICTIONS LSTM STRICT =====")
print(comparison.sort_values("error_time_min", ascending=False).head(10).to_string(index=False))

print("\nDone.")