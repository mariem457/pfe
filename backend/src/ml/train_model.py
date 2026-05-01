"""
train_model_v2.py
=================
XGBoost - Smart Waste Management - Paris 15e
Adapté au dataset final_dataset_paris15_v2.csv

Corrections vs ancien script :
  - fill_level RETIRÉ des features (data leakage principal)
  - fill_rate RETIRÉ des features directes (leakage secondaire)
  - Ajout features puissantes : hours_since_collection, météo, zone, type_bin
  - Rolling 7 jours correct (calé sur colonne 'time', pas timestamp)
  - Time-aware train/test split (pas de random split sur time series)
  - GridSearch optimisé (moins de combos, plus intelligents)
  - Feature importance exportée
  - Résultats sauvegardés dans comparison.csv
"""

import pandas as pd
import numpy as np
import joblib
from xgboost import XGBRegressor
from sklearn.model_selection import TimeSeriesSplit, GridSearchCV
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.preprocessing import LabelEncoder

print("=" * 60)
print("SCRIPT VERSION = TRAIN_V2_NO_LEAKAGE_TIMESERIES")
print("=" * 60)

# ─── LOAD ─────────────────────────────────────────────────────
df = pd.read_csv("final_dataset_paris15_v2.csv")
df.columns = df.columns.str.strip()

df["time"] = pd.to_datetime(df["time"])
df = df.sort_values(["bin_id", "time"]).reset_index(drop=True)

print(f"Dataset loaded: {df.shape[0]} rows, {df['bin_id'].nunique()} bins")

# ─── CLEANING ─────────────────────────────────────────────────
# Garder seulement les lectures normales (pas anomalies capteur)
if "anomaly_type" in df.columns:
    df = df[df["anomaly_type"] == "none"].copy()
    print(f"After anomaly filter: {df.shape[0]} rows")

df = df[(df["battery_level"] >= 0) & (df["battery_level"] <= 100)]
df = df[(df["fill_level"] >= 0) & (df["fill_level"] <= 100)]
df = df[df["rssi"].notna()]
df = df[df["fill_level"].notna()]

# ─── TARGET ───────────────────────────────────────────────────
# Prédire fill_level à t+1
df["fill_level_next"] = df.groupby("bin_id")["fill_level"].shift(-1)
df["next_collected"]  = df.groupby("bin_id")["collected"].shift(-1)

# Garder seulement les rows où la prochaine heure n'est PAS une collecte
# (on veut prédire le remplissage naturel, pas le reset post-collecte)
df = df.dropna(subset=["fill_level_next", "next_collected"])
df = df[df["next_collected"] == 0]

print(f"After target engineering: {df.shape[0]} rows")

# ─── ENCODE CATEGORICALS ──────────────────────────────────────
le_zone    = LabelEncoder()
le_type    = LabelEncoder()
le_act     = LabelEncoder()

df["zone_enc"]     = le_zone.fit_transform(df["zone"])
df["type_bin_enc"] = le_type.fit_transform(df["type_bin"])
df["activity_enc"] = le_act.fit_transform(df["activity_level"])

# ─── LAG FEATURES ─────────────────────────────────────────────
# Passé récent (court terme)
df["fill_level_lag1"] = df.groupby("bin_id")["fill_level"].shift(1)
df["fill_level_lag2"] = df.groupby("bin_id")["fill_level"].shift(2)
df["fill_level_lag3"] = df.groupby("bin_id")["fill_level"].shift(3)
df["fill_rate_lag1"]  = df.groupby("bin_id")["fill_rate"].shift(1)
df["fill_rate_lag2"]  = df.groupby("bin_id")["fill_rate"].shift(2)
df["weight_kg_lag1"]  = df.groupby("bin_id")["weight_kg"].shift(1)
df["rssi_lag1"]       = df.groupby("bin_id")["rssi"].shift(1)

# Delta (tendance court terme)
df["fill_delta_1h"]   = df["fill_level_lag1"] - df["fill_level_lag2"]
df["fill_delta_2h"]   = df["fill_level_lag1"] - df.groupby("bin_id")["fill_level"].shift(3)

# ─── ROLLING 7 JOURS ──────────────────────────────────────────
# 7j * 24h = 168 points (données horaires)
WINDOW = 168

df["fill_mean_7d"]   = df.groupby("bin_id")["fill_level"].transform(
    lambda x: x.shift(1).rolling(WINDOW, min_periods=24).mean()
)
df["fill_max_7d"]    = df.groupby("bin_id")["fill_level"].transform(
    lambda x: x.shift(1).rolling(WINDOW, min_periods=24).max()
)
df["fill_std_7d"]    = df.groupby("bin_id")["fill_level"].transform(
    lambda x: x.shift(1).rolling(WINDOW, min_periods=24).std()
)
df["fill_rate_mean_7d"] = df.groupby("bin_id")["fill_rate"].transform(
    lambda x: x.shift(1).rolling(WINDOW, min_periods=24).mean()
)

# Tendance long terme (fill actuel vs moyenne 7j)
df["fill_trend_7d"]  = df["fill_level_lag1"] - df["fill_mean_7d"]

# ─── FEATURES TEMPS CYCLIQUES ─────────────────────────────────
df["hour_sin"]       = np.sin(2 * np.pi * df["hour"] / 24)
df["hour_cos"]       = np.cos(2 * np.pi * df["hour"] / 24)
df["dow_sin"]        = np.sin(2 * np.pi * df["day_of_week"] / 7)
df["dow_cos"]        = np.cos(2 * np.pi * df["day_of_week"] / 7)
df["month_sin"]      = np.sin(2 * np.pi * df["month"] / 12)
df["month_cos"]      = np.cos(2 * np.pi * df["month"] / 12)

# ─── FEATURES LISTE FINALE ────────────────────────────────────
features = [
    # ⚠️  fill_level RETIRÉ → c'était la source du leakage R²=0.9999
    # ⚠️  fill_rate RETIRÉ (version directe) → leakage secondaire

    # Passé récent (lag)
    "fill_level_lag1",        # fill_level il y a 1h
    "fill_level_lag2",        # fill_level il y a 2h
    "fill_level_lag3",        # fill_level il y a 3h
    "fill_rate_lag1",         # vitesse de remplissage il y a 1h
    "fill_rate_lag2",         # vitesse de remplissage il y a 2h
    "weight_kg_lag1",         # poids il y a 1h
    "rssi_lag1",              # signal il y a 1h

    # Tendance
    "fill_delta_1h",          # delta entre lag1 et lag2
    "fill_delta_2h",          # delta entre lag1 et lag3

    # Mémoire long terme (7 jours)
    "fill_mean_7d",           # moyenne semaine
    "fill_max_7d",            # pic semaine
    "fill_std_7d",            # variabilité semaine
    "fill_rate_mean_7d",      # vitesse moyenne semaine
    "fill_trend_7d",          # tendance vs moyenne

    # Contexte collecte
    "hours_since_collection", # ⭐ feature clé nouvelle
    "collected",              # 0/1 collecte à t

    # Capteurs temps réel (pas de leakage ici)
    "battery_level",
    "rssi",

    # Météo (données réelles)
    "temp",
    "rhum",
    "prcp",
    "wspd",

    # Temps cyclique
    "hour_sin",
    "hour_cos",
    "dow_sin",
    "dow_cos",
    "month_sin",
    "month_cos",
    "is_weekend",

    # Zone / type (encodés)
    "zone_enc",
    "type_bin_enc",
    "activity_enc",

    # Géographie
    "density_hab_km2",
    "commerce_count",
]

# ─── VÉRIFICATION ─────────────────────────────────────────────
required = features + ["fill_level_next"]
missing  = [c for c in required if c not in df.columns]
print("Missing columns:", missing)
if missing:
    raise ValueError(f"Missing: {missing}")

df = df.dropna(subset=required)
print(f"Final dataset shape: {df.shape}")

# ─── TRAIN / TEST SPLIT (TIME-AWARE) ──────────────────────────
# ⚠️  On NE FAIT PAS random split sur time series
# On coupe : 80% premiers mois → train | 20% derniers mois → test
# Évite le "future leakage" dans la cross-validation

cutoff = df["time"].quantile(0.80)
train_df = df[df["time"] <= cutoff]
test_df  = df[df["time"] >  cutoff]

X_train = train_df[features]
y_train = train_df["fill_level_next"]
X_test  = test_df[features]
y_test  = test_df["fill_level_next"]

print(f"\nTrain: {len(X_train)} rows (jusqu'à {cutoff.date()})")
print(f"Test:  {len(X_test)} rows  (après {cutoff.date()})")

# ─── GRID SEARCH (TimeSeriesSplit) ────────────────────────────
# TimeSeriesSplit = cross-validation respectant l'ordre temporel
tscv = TimeSeriesSplit(n_splits=3)

param_grid = {
    "n_estimators":    [400, 600],
    "max_depth":       [5, 7, 9],
    "learning_rate":   [0.03, 0.05],
    "subsample":       [0.8],
    "colsample_bytree":[0.8],
    "min_child_weight":[3, 5],    # évite overfitting
}

grid = GridSearchCV(
    estimator=XGBRegressor(
        objective="reg:squarederror",
        random_state=42,
        n_jobs=-1,
    ),
    param_grid=param_grid,
    cv=tscv,
    scoring="neg_mean_absolute_error",
    verbose=2,
    n_jobs=1,   # GridSearch parallel, XGB internal parallel
)

print("\nStart GridSearch (TimeSeriesSplit)...")
grid.fit(X_train, y_train)

model = grid.best_estimator_
print(f"\nBest parameters: {grid.best_params_}")
print(f"Best CV MAE:     {-grid.best_score_:.4f}")

# ─── EVALUATION ───────────────────────────────────────────────
y_pred = model.predict(X_test)
y_pred = y_pred.clip(0, 100)

mae  = mean_absolute_error(y_test, y_pred)
r2   = r2_score(y_test, y_pred)
rmse = np.sqrt(np.mean((y_test - y_pred) ** 2))

print("\n" + "=" * 40)
print(f"MAE  : {mae:.4f}  (objectif < 5)")
print(f"RMSE : {rmse:.4f}")
print(f"R²   : {r2:.4f}   (objectif > 0.85, réaliste)")
print("=" * 40)

# ─── FEATURE IMPORTANCE ───────────────────────────────────────
importance_df = pd.DataFrame({
    "feature":   features,
    "importance": model.feature_importances_,
}).sort_values("importance", ascending=False)

print("\nTop 15 features:")
print(importance_df.head(15).to_string(index=False))
importance_df.to_csv("feature_importance.csv", index=False)

# ─── SAVE ─────────────────────────────────────────────────────
joblib.dump(model, "smart_bin_model_v2.pkl")
print("\nModel saved: smart_bin_model_v2.pkl")

comparison = pd.DataFrame({
    "time":      test_df["time"].values,
    "bin_id":    test_df["bin_id"].values,
    "zone":      test_df["zone"].values,
    "real":      y_test.values,
    "predicted": y_pred,
    "error":     np.abs(y_test.values - y_pred),
})
comparison.to_csv("comparison.csv", index=False)
print("Comparison saved: comparison.csv")

print("\nSample predictions:")
print(comparison[["bin_id", "zone", "real", "predicted", "error"]].head(15).to_string(index=False))