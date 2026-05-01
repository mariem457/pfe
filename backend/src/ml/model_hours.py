import pandas as pd
import joblib
import numpy as np
from xgboost import XGBRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error, r2_score

print("SCRIPT VERSION = TRAIN_TIME_TO_THRESHOLD_MODEL")

# -----------------------------
# Load dataset
# -----------------------------
df = pd.read_csv("dataset_pfe.csv")
df.columns = df.columns.str.strip()

# -----------------------------
# Parse / sort
# -----------------------------
if "timestamp" in df.columns:
    df["timestamp"] = pd.to_datetime(df["timestamp"])

df = df.sort_values(["bin_id", "timestamp"]).reset_index(drop=True)

# -----------------------------
# Build fill_rate if missing
# -----------------------------
if "fill_rate" not in df.columns:
    time_diff = df.groupby("bin_id")["timestamp"].diff().dt.total_seconds() / 3600
    fill_diff = df.groupby("bin_id")["fill_level"].diff()
    df["fill_rate"] = (fill_diff / time_diff).fillna(0)

# -----------------------------
# Cleaning
# -----------------------------
if "anomaly_type" in df.columns:
    df = df[df["anomaly_type"] == "none"]

df = df[(df["battery_level"] >= 0) & (df["battery_level"] <= 100)]
df = df[(df["fill_level"] >= 0) & (df["fill_level"] <= 100)]
df = df[df["rssi"].notna()]

# نخلي فقط الحالات اللي fill_rate فيها منطقية
df = df[df["fill_rate"] > 0]

# نخلي فقط الحالات اللي مازال threshold ما توصلتش
df = df[df["fill_level"] < df["collection_threshold"]]

# -----------------------------
# Target engineering
# -----------------------------
df["time_to_threshold_hours"] = (
    (df["collection_threshold"] - df["fill_level"]) / df["fill_rate"]
)

# ننحي القيم غير المنطقية
df = df[df["time_to_threshold_hours"] >= 0]
df = df[df["time_to_threshold_hours"] <= 168]  # max 7 days

# -----------------------------
# Features
# نخلي فقط features اللي تنجم تلقاهم في BD
# -----------------------------
features = [
    "hour",
    "fill_level",
    "fill_rate",
    "battery_level",
    "weight_kg",
    "rssi",
    "collected"
]

target = "time_to_threshold_hours"

required_cols = features + [target]
missing_cols = [col for col in required_cols if col not in df.columns]

print("Missing columns:", missing_cols)
if missing_cols:
    raise ValueError(f"Missing columns in dataset: {missing_cols}")

df = df.dropna(subset=required_cols)

print("Dataset shape:", df.shape)

X = df[features]
y = df[target]

# -----------------------------
# Split
# -----------------------------
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)

print("Start training...")

# -----------------------------
# Model
# -----------------------------
model = XGBRegressor(
    n_estimators=400,
    max_depth=6,
    learning_rate=0.05,
    subsample=0.8,
    colsample_bytree=0.8,
    random_state=42,
    objective="reg:squarederror"
)

model.fit(X_train, y_train)

print("Training finished.")

# -----------------------------
# Evaluation
# -----------------------------
y_pred = model.predict(X_test)
y_pred = np.maximum(0, y_pred)

mae = mean_absolute_error(y_test, y_pred)
r2 = r2_score(y_test, y_pred)

print("MAE (hours):", mae)
print("R2:", r2)

# -----------------------------
# Save
# -----------------------------
joblib.dump(model, "smart_bin_time_to_threshold_model.pkl")
print("Model saved: smart_bin_time_to_threshold_model.pkl")

comparison = pd.DataFrame({
    "real_hours": y_test,
    "predicted_hours": y_pred
})

print(comparison.head(10))