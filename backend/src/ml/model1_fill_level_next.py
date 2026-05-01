"""
model1_fill_level_next.py
==========================
MODÈLE 1 — Prédiction du fill_level à t+1
Smart Waste Management — Paris 15e
Dataset : final_dataset_paris15_v2.csv

But : Prédire le niveau de remplissage à la prochaine heure
      → Permet au système de savoir où en est chaque bac en temps réel
"""

import pandas as pd
import numpy as np
import joblib
from xgboost import XGBRegressor
from sklearn.model_selection import TimeSeriesSplit, GridSearchCV
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.preprocessing import LabelEncoder

print("=" * 60)
print("MODÈLE 1 — fill_level_next (t+1)")
print("=" * 60)

# ─── LOAD ──────────────────────────────────────────────────────
df = pd.read_csv("final_dataset_paris15_v2.csv")
df.columns = df.columns.str.strip()
df["time"] = pd.to_datetime(df["time"])
df = df.sort_values(["bin_id", "time"]).reset_index(drop=True)
print(f"Dataset chargé : {df.shape[0]} lignes, {df['bin_id'].nunique()} bacs")

# ─── CLEANING ──────────────────────────────────────────────────
if "anomaly_type" in df.columns:
    df = df[df["anomaly_type"] == "none"].copy()
df = df[(df["battery_level"] >= 0) & (df["battery_level"] <= 100)]
df = df[(df["fill_level"] >= 0) & (df["fill_level"] <= 100)]
df = df[df["rssi"].notna() & df["fill_level"].notna()]
print(f"Après nettoyage : {df.shape[0]} lignes")

# ─── TARGET ────────────────────────────────────────────────────
df["fill_level_next"] = df.groupby("bin_id")["fill_level"].shift(-1)
df["next_collected"]  = df.groupby("bin_id")["collected"].shift(-1)
df = df.dropna(subset=["fill_level_next", "next_collected"])
# Garder seulement les cas où le prochain état n'est PAS une collecte
df = df[df["next_collected"] == 0]
print(f"Après target engineering : {df.shape[0]} lignes")

# ─── ENCODAGE CATÉGORIEL ───────────────────────────────────────
le_zone = LabelEncoder()
le_type = LabelEncoder()
le_act  = LabelEncoder()
df["zone_enc"]     = le_zone.fit_transform(df["zone"])
df["type_bin_enc"] = le_type.fit_transform(df["type_bin"])
df["activity_enc"] = le_act.fit_transform(df["activity_level"])

# ─── LAG FEATURES ──────────────────────────────────────────────
for lag in [1, 2, 3]:
    df[f"fill_level_lag{lag}"] = df.groupby("bin_id")["fill_level"].shift(lag)
    df[f"fill_rate_lag{lag}"]  = df.groupby("bin_id")["fill_rate"].shift(lag)

df["weight_kg_lag1"] = df.groupby("bin_id")["weight_kg"].shift(1)
df["rssi_lag1"]      = df.groupby("bin_id")["rssi"].shift(1)

# Tendance court terme
df["fill_delta_1h"] = df["fill_level_lag1"] - df["fill_level_lag2"]
df["fill_delta_2h"] = df["fill_level_lag1"] - df["fill_level_lag3"]

# ─── ROLLING 7 JOURS (168h) ────────────────────────────────────
W = 168
df["fill_mean_7d"]      = df.groupby("bin_id")["fill_level"].transform(
    lambda x: x.shift(1).rolling(W, min_periods=24).mean())
df["fill_max_7d"]       = df.groupby("bin_id")["fill_level"].transform(
    lambda x: x.shift(1).rolling(W, min_periods=24).max())
df["fill_std_7d"]       = df.groupby("bin_id")["fill_level"].transform(
    lambda x: x.shift(1).rolling(W, min_periods=24).std())
df["fill_rate_mean_7d"] = df.groupby("bin_id")["fill_rate"].transform(
    lambda x: x.shift(1).rolling(W, min_periods=24).mean())
df["fill_trend_7d"]     = df["fill_level_lag1"] - df["fill_mean_7d"]

# ─── TEMPS CYCLIQUE ────────────────────────────────────────────
df["hour_sin"]  = np.sin(2 * np.pi * df["hour"] / 24)
df["hour_cos"]  = np.cos(2 * np.pi * df["hour"] / 24)
df["dow_sin"]   = np.sin(2 * np.pi * df["day_of_week"] / 7)
df["dow_cos"]   = np.cos(2 * np.pi * df["day_of_week"] / 7)
df["month_sin"] = np.sin(2 * np.pi * df["month"] / 12)
df["month_cos"] = np.cos(2 * np.pi * df["month"] / 12)

# ─── FEATURES ──────────────────────────────────────────────────
features = [
    # Passé récent
    "fill_level_lag1", "fill_level_lag2", "fill_level_lag3",
    "fill_rate_lag1",  "fill_rate_lag2",  "fill_rate_lag3",
    "weight_kg_lag1",  "rssi_lag1",
    # Tendances
    "fill_delta_1h", "fill_delta_2h",
    # Mémoire 7 jours
    "fill_mean_7d", "fill_max_7d", "fill_std_7d",
    "fill_rate_mean_7d", "fill_trend_7d",
    # Contexte collecte
    "hours_since_collection", "collected",
    # Capteurs
    "battery_level", "rssi",
    # Météo réelle
    "temp", "rhum", "prcp", "wspd",
    # Temps cyclique
    "hour_sin", "hour_cos", "dow_sin", "dow_cos",
    "month_sin", "month_cos", "is_weekend",
    # Zone / type
    "zone_enc", "type_bin_enc", "activity_enc",
    # Géographie
    "density_hab_km2", "commerce_count",
]

df = df.dropna(subset=features + ["fill_level_next"])
print(f"Shape finale : {df.shape}")

# ─── TRAIN / TEST SPLIT TEMPOREL ───────────────────────────────
cutoff   = df["time"].quantile(0.80)
train_df = df[df["time"] <= cutoff]
test_df  = df[df["time"] >  cutoff]
X_train, y_train = train_df[features], train_df["fill_level_next"]
X_test,  y_test  = test_df[features],  test_df["fill_level_next"]
print(f"\nTrain : {len(X_train)} lignes (jusqu'au {cutoff.date()})")
print(f"Test  : {len(X_test)} lignes  (après le {cutoff.date()})")

# ─── GRID SEARCH ───────────────────────────────────────────────
tscv = TimeSeriesSplit(n_splits=3)
param_grid = {
    "n_estimators":    [300, 500],
    "max_depth":       [5, 7],
    "learning_rate":   [0.05, 0.08],
    "subsample":       [0.8],
    "colsample_bytree":[0.8],
    "min_child_weight":[3, 5],
}
grid = GridSearchCV(
    XGBRegressor(objective="reg:squarederror", random_state=42, n_jobs=-1),
    param_grid, cv=tscv, scoring="neg_mean_absolute_error",
    verbose=1, n_jobs=1
)
print("\nDémarrage GridSearch...")
grid.fit(X_train, y_train)
model1 = grid.best_estimator_
print(f"Meilleurs paramètres : {grid.best_params_}")

# ─── ÉVALUATION ────────────────────────────────────────────────
y_pred = model1.predict(X_test).clip(0, 100)
mae  = mean_absolute_error(y_test, y_pred)
r2   = r2_score(y_test, y_pred)
rmse = np.sqrt(np.mean((y_test - y_pred) ** 2))

print("\n" + "=" * 50)
print(f"  MODÈLE 1 — fill_level_next")
print(f"  MAE  : {mae:.4f} %")
print(f"  RMSE : {rmse:.4f} %")
print(f"  R²   : {r2:.4f}")
print("=" * 50)

# ─── FEATURE IMPORTANCE ────────────────────────────────────────
imp = pd.DataFrame({
    "feature": features,
    "importance": model1.feature_importances_
}).sort_values("importance", ascending=False)
print("\nTop 10 features :")
print(imp.head(10).to_string(index=False))
imp.to_csv("feature_importance_model1.csv", index=False)

# ─── SAUVEGARDE ────────────────────────────────────────────────
joblib.dump(model1, "model1_fill_level_next.pkl")
joblib.dump(le_zone, "le_zone_m1.pkl")
joblib.dump(le_type, "le_type_m1.pkl")
joblib.dump(le_act,  "le_act_m1.pkl")
print("\nModèle sauvegardé : model1_fill_level_next.pkl")

comp = pd.DataFrame({
    "time": test_df["time"].values,
    "bin_id": test_df["bin_id"].values,
    "zone": test_df["zone"].values,
    "fill_level_reel": y_test.values,
    "fill_level_predit": y_pred,
    "erreur_abs": np.abs(y_test.values - y_pred),
})
comp.to_csv("comparison_model1.csv", index=False)
print("Comparaison sauvegardée : comparison_model1.csv")

print("\nExemple prédictions :")
print(comp[["bin_id","zone","fill_level_reel","fill_level_predit","erreur_abs"]].head(10).to_string(index=False))