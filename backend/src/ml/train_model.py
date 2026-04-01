import pandas as pd
import joblib
from xgboost import XGBRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error, r2_score

print("SCRIPT VERSION = TRAIN_BD_FEATURES_WITH_LAGS")

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
# Base engineering
# -----------------------------
if "fill_rate" not in df.columns:
    time_diff = df.groupby("bin_id")["timestamp"].diff().dt.total_seconds() / 3600
    fill_diff = df.groupby("bin_id")["fill_level"].diff()
    df["fill_rate"] = (fill_diff / time_diff).fillna(0)

df["fill_level_next"] = df.groupby("bin_id")["fill_level"].shift(-1)
df["next_collected"] = df.groupby("bin_id")["collected"].shift(-1)

# -----------------------------
# Filtering
# -----------------------------
df = df.dropna(subset=["fill_level_next", "next_collected"])
df = df[df["next_collected"] == 0]

if "anomaly_type" in df.columns:
    df = df[df["anomaly_type"] == "none"]

df = df[(df["battery_level"] >= 0) & (df["battery_level"] <= 100)]
df = df[(df["fill_level"] >= 0) & (df["fill_level"] <= 100)]
df = df[df["rssi"].notna()]

# -----------------------------
# Lag features (important)
# -----------------------------
df["fill_level_lag1"] = df.groupby("bin_id")["fill_level"].shift(1)
df["fill_level_lag2"] = df.groupby("bin_id")["fill_level"].shift(2)

df["fill_rate_lag1"] = df.groupby("bin_id")["fill_rate"].shift(1)
df["weight_kg_lag1"] = df.groupby("bin_id")["weight_kg"].shift(1)
df["rssi_lag1"] = df.groupby("bin_id")["rssi"].shift(1)

# -----------------------------
# Features
# -----------------------------
features = [
    "hour",
    "fill_level",
    "fill_rate",
    "battery_level",
    "weight_kg",
    "rssi",
    "collected",
    "fill_level_lag1",
    "fill_level_lag2",
    "fill_rate_lag1",
    "weight_kg_lag1",
    "rssi_lag1"
]

required_cols = features + ["fill_level_next"]
missing_cols = [col for col in required_cols if col not in df.columns]

print("Missing columns:", missing_cols)
if missing_cols:
    raise ValueError(f"Missing columns in dataset: {missing_cols}")

df = df.dropna(subset=required_cols)

print("Dataset shape after filtering:", df.shape)

# -----------------------------
# Train / test
# -----------------------------
X = df[features]
y = df["fill_level_next"]

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)

print("Start training...")

model = XGBRegressor(
    n_estimators=500,
    max_depth=7,
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
y_pred = y_pred.clip(0, 100)

mae = mean_absolute_error(y_test, y_pred)
r2 = r2_score(y_test, y_pred)

print("MAE:", mae)
print("R2:", r2)

joblib.dump(model, "smart_bin_model_bd_lags.pkl")
print("Model saved: smart_bin_model_bd_lags.pkl")

comparison = pd.DataFrame({
    "real": y_test,
    "predicted": y_pred
})

print(comparison.head(10))
