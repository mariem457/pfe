# =========================================================
# XGBOOST FINAL — DUAL TARGETS WITH WEEKLY HISTORY
# fill_level_next + time_to_90_real
# =========================================================

import joblib
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

from pathlib import Path

from sklearn.preprocessing import StandardScaler
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.multioutput import MultiOutputRegressor

from xgboost import XGBRegressor


# =========================================================
# CONFIG
# =========================================================
# Dossier courant du script.
BASE_DIR = Path.cwd()


# Fichier CSV contenant les données d'entraînement.
DATA_FILE = BASE_DIR / "final_dataset_paris15_v7_realistic.csv"


# Dossier de sauvegarde des fichiers générés.
MODEL_DIR = BASE_DIR / "models_xgboost_final"

# Création du dossier s'il n'existe pas.
MODEL_DIR.mkdir(parents=True, exist_ok=True)




# Fichier de sauvegarde du modèle entraîné.
MODEL_FILE = MODEL_DIR / "xgboost_dual_target.pkl"

# Fichier de sauvegarde du scaler.
SCALER_FILE = MODEL_DIR / "scaler_xgboost.pkl"


# Fichier de comparaison entre les valeurs réelles et prédites.
COMPARISON_FILE = MODEL_DIR / "comparison_xgboost_dual.csv"


# Valeur fixe pour obtenir des résultats reproductibles.
RANDOM_SEED = 42


# Première sortie du modèle : prochain niveau de remplissage
TARGET_FILL = "fill_level_next"

# Deuxième sortie du modèle : temps restant avant 90 %.
TARGET_TIME = "time_to_90_real"







# FULL_THRESHOLD représente le seuil critique choisi dans le projet.
# À partir de 90%, une poubelle est considérée comme presque pleine
# et peut nécessiter une collecte prioritaire.
FULL_THRESHOLD = 90.0




# TIME_MAX limite la valeur maximale du temps prédit.
# Cela évite d'avoir des prédictions trop grandes ou peu réalistes.
TIME_MAX = 36.0


# =========================================================
# LOAD DATA
# =========================================================
if not DATA_FILE.exists():
    raise FileNotFoundError(f"Dataset not found: {DATA_FILE}")

df = pd.read_csv(DATA_FILE)

# Supprime les espaces inutiles au début et à la fin des noms de colonnes.
df.columns = df.columns.str.strip()

if "time" in df.columns:
    df = df.rename(columns={"time": "timestamp"})

df["timestamp"] = pd.to_datetime(df["timestamp"], errors="coerce")
df = df.dropna(subset=["timestamp"])

df = df.sort_values(["bin_id", "timestamp"]).reset_index(drop=True)

df = df.dropna(subset=["bin_id", "fill_level"]).copy()

df["fill_level"] = pd.to_numeric(df["fill_level"], errors="coerce")
df = df.dropna(subset=["fill_level"])

df = df[(df["fill_level"] >= 0) & (df["fill_level"] <= 100)].copy()

#Ce bloc nettoie deux informations importantes : les anomalies et les collectes. 
#les anomalies sont supprimées pour éviter d’entraîner le modèle sur des mesures anormales
#La colonne collected est transformée en 0 ou 1 afin de détecter les moments où une poubelle a été vidée.
# =========================================================
# CLEANING
# =========================================================
if "anomaly_type" in df.columns:
    df["has_anomaly"] = (
        df["anomaly_type"]
        .astype(str)
        .str.lower()
        .ne("none")
        .astype(int)
    )
else:
    df["has_anomaly"] = 0


if "collected" in df.columns:
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
else:
    df["collected"] = 0


df = df[df["has_anomaly"] == 0].copy()



# Cette partie détecte les nouveaux cycles de remplissage après chaque collecte.
# Elle permet de séparer l'historique de chaque poubelle en périodes normales
# de remplissage afin d'entraîner le modèle sur des données cohérentes.
# =========================================================
# CYCLE DETECTION
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
    (df["collected"] == 0) &
    (df["reset_detected"] == 0)
].copy()

# IMPORTANT:
# We do NOT force 168h because most cycles do not last a full week.
# We keep minimum 24h and use weekly rolling features when available.
df = df[df["hours_from_cycle_start"] >= 24].copy()








# Cette partie crée des variables temporelles afin d'aider le modèle à comprendre
# l'effet de l'heure, du jour, du mois et du week-end sur le remplissage.
# Les variables sin/cos permettent de représenter le temps comme un cycle.
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








# Cette partie transforme les variables catégorielles en valeurs numériques,
# car le modèle XGBoost ne peut pas utiliser directement des textes comme la zone,
# le type de poubelle ou le niveau d'activité. Chaque catégorie est donc encodée
# sous forme de nombre afin d'être exploitable pendant l'entraînement.
# =========================================================
# ENCODING
# =========================================================
df["zone_enc"] = (
    df["zone"].astype("category").cat.codes
    if "zone" in df.columns else 0
)

df["type_bin_enc"] = (
    df["type_bin"].astype("category").cat.codes
    if "type_bin" in df.columns else 0
)

if "activity_level" in df.columns:
    df["activity_enc"] = (
        df["activity_level"]
        .map({"Low": 0, "Medium": 1, "High": 2})
        .fillna(1)
        .astype(int)
    )
else:
    df["activity_enc"] = 1







# Cette partie extrait l'historique récent du niveau de remplissage.
# Elle permet au modèle d'apprendre l'évolution du bac sur 1h, 6h et 24h
# afin de mieux prédire le prochain remplissage.

# =========================================================
# FEATURE ENGINEERING — SHORT HISTORY
# =========================================================
df["fill_rate"] = (
    df.groupby(["bin_id", "cycle_id"])["fill_level"]
    .diff()
    .clip(lower=0)
    .fillna(0)
)

df["fill_lag_1"] = (
    df.groupby(["bin_id", "cycle_id"])["fill_level"].shift(1)
)

df["fill_lag_6"] = (
    df.groupby(["bin_id", "cycle_id"])["fill_level"].shift(6)
)

df["fill_lag_24"] = (
    df.groupby(["bin_id", "cycle_id"])["fill_level"].shift(24)
)

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








# Cette partie exploite l'historique hebdomadaire du remplissage sur 168 heures,
# c'est-à-dire environ 7 jours. Elle permet au modèle de prendre en compte
# les habitudes répétitives d'une poubelle sur une semaine, comme les jours
# ou les périodes où le remplissage est généralement plus élevé.
# =========================================================
# FEATURE ENGINEERING — WEEKLY HISTORY 168H
# =========================================================
df["fill_lag_168"] = (
    df.groupby(["bin_id", "cycle_id"])["fill_level"].shift(168)
)

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

# Cette partie définit la première cible du modèle : le prochain niveau de remplissage.
# Pour chaque poubelle et chaque cycle, on décale la colonne fill_level d'une ligne vers le haut
# afin que chaque mesure actuelle soit associée au niveau de remplissage de la mesure suivante.
# Le modèle apprend donc à prédire l'état futur immédiat de la poubelle.
# =========================================================
# TARGET 1 — NEXT FILL LEVEL
# =========================================================
df[TARGET_FILL] = (
    df.groupby(["bin_id", "cycle_id"])["fill_level"]
    .shift(-1)
)



# Cette partie crée la deuxième cible du modèle : le temps restant avant d'atteindre 90%.
# Pour chaque mesure, le code cherche dans les mesures futures du même cycle le premier moment
# où le niveau de remplissage atteint le seuil critique. Si le bac est déjà à 90% ou plus,
# le temps restant est égal à 0. Les lignes où le seuil n'est jamais atteint sont supprimées,
# puis les valeurs sont limitées à des plages réalistes.
# =========================================================
# TARGET 2 — TIME TO 90%
# =========================================================
def compute_time_to_threshold(group, threshold=90.0):
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


df = df.groupby(
    ["bin_id", "cycle_id"],
    group_keys=False
).apply(compute_time_to_threshold)


df = df.dropna(subset=[TARGET_FILL, TARGET_TIME]).copy()

df[TARGET_FILL] = df[TARGET_FILL].clip(0, 100)
df[TARGET_TIME] = df[TARGET_TIME].clip(0, TIME_MAX)




# Cette partie remplace les valeurs manquantes par des valeurs par défaut.
# Elle permet de garder un dataset complet et exploitable pour l'entraînement du modèle.
# =========================================================
# DEFAULT VALUES
# =========================================================
defaults = {
    "battery_level": 80,
    "rssi": -70,
    "weight_kg": df["fill_level"] * 0.09,

    # Same as your old model to stay consistent
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

# Cette partie définit les variables d'entrée du modèle XGBoost.
# Ces features décrivent l'état actuel du bac, son historique, le temps,
# la météo et le contexte. Leur ordre doit rester identique dans le script
# d'entraînement et dans le script de prédiction réelle.
# =========================================================
# FEATURES — IMPORTANT: SAME ORDER WILL BE USED IN predict_from_db.py
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

    # Weekly history features
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

# Cette partie prépare les features en supprimant les valeurs infinies
# et en remplaçant les valeurs manquantes par 0 pour garantir un entraînement stable.
# =========================================================
# CLEAN FEATURES
# =========================================================
df[features] = (
    df[features]
    .replace([np.inf, -np.inf], np.nan)
    .fillna(0)
)


# =========================================================
# CHECK DATA SIZE
# =========================================================
print("\n===== DATASET AFTER FEATURE ENGINEERING =====")
print(f"Rows: {len(df)}")
print(f"Bins: {df['bin_id'].nunique()}")
print(f"From: {df['timestamp'].min()}")
print(f"To  : {df['timestamp'].max()}")
print(f"Features count: {len(features)}")






# Cette partie effectue une séparation temporelle du dataset.
# Le modèle apprend sur les anciennes données et il est testé sur les données récentes,
# ce qui permet de simuler une prédiction réelle basée sur l'historique.
# =========================================================
# TEMPORAL SPLIT
# =========================================================
df = df.sort_values("timestamp").reset_index(drop=True)

split_idx = int(len(df) * 0.80)

train_df = df.iloc[:split_idx].copy()
test_df = df.iloc[split_idx:].copy()

X_train = train_df[features]
X_test = test_df[features]

y_train = train_df[[TARGET_FILL, TARGET_TIME]]
y_test = test_df[[TARGET_FILL, TARGET_TIME]]



# Cette partie normalise les données d'entrée.
# Le scaler est ajusté sur X_train puis appliqué à X_train et X_test
# pour garder la même échelle entre l'entraînement et le test.
# =========================================================
# SCALING
# =========================================================
scaler = StandardScaler()

X_train_s = scaler.fit_transform(X_train)
X_test_s = scaler.transform(X_test)





# Cette partie définit le modèle XGBoost utilisé pour la régression.
# Les paramètres choisis contrôlent le nombre d'arbres, leur profondeur,
# la vitesse d'apprentissage et la proportion des données utilisées à chaque étape.
# Le modèle apprend progressivement à partir des features afin de prédire
# le prochain niveau de remplissage et le temps restant avant 90%.
# =========================================================
# XGBOOST MODEL
# =========================================================
base_model = XGBRegressor(
    n_estimators=700,
    max_depth=6,
    learning_rate=0.03,
    subsample=0.85,
    colsample_bytree=0.85,
    objective="reg:squarederror",
    random_state=RANDOM_SEED,
    n_jobs=-1,
)





# =========================================================
# MULTI-OUTPUT MODEL
# =========================================================
# XGBRegressor prédit normalement une seule valeur.
# Comme notre problème contient deux cibles à prédire,
# nous utilisons MultiOutputRegressor.
#
# Il crée en interne deux modèles XGBoost :
# - le premier apprend à prédire fill_level_next ;
# - le second apprend à prédire time_to_90_real.
#
# Cela nous permet de comparer les deux prédictions pendant la phase d'évaluation.

model = MultiOutputRegressor(base_model)

model.fit(X_train_s, y_train)


# =========================================================
# PREDICTION
# =========================================================



# =========================================================
# PREDICTION ON TEST DATA
# =========================================================
# Le modèle retourne deux colonnes de prédiction :
# pred[:, 0] correspond à fill_level_next.
# pred[:, 1] correspond à time_to_90_real.
#
# Les valeurs sont ensuite limitées avec np.clip pour rester dans des plages réalistes :
# - le remplissage entre 0% et 100% ;
# - le temps entre 0h et TIME_MAX.
pred = model.predict(X_test_s)

fill_true = y_test[TARGET_FILL].values
fill_pred = np.clip(pred[:, 0], 0, 100)

time_true = y_test[TARGET_TIME].values
time_pred = np.clip(pred[:, 1], 0, TIME_MAX)


# =========================================================
# METRICS
# =========================================================
fill_mae = mean_absolute_error(fill_true, fill_pred)
fill_rmse = np.sqrt(mean_squared_error(fill_true, fill_pred))
fill_r2 = r2_score(fill_true, fill_pred)

time_mae = mean_absolute_error(time_true, time_pred)
time_rmse = np.sqrt(mean_squared_error(time_true, time_pred))
time_r2 = r2_score(time_true, time_pred)


print("\n===== FINAL METRICS XGBOOST WITH WEEKLY HISTORY =====")

print("\nFill Level Next")
print(f"MAE  : {fill_mae:.4f}")
print(f"RMSE : {fill_rmse:.4f}")
print(f"R²   : {fill_r2:.4f}")

print("\nTime To Full")
print(f"MAE  : {time_mae:.4f} h")
print(f"RMSE : {time_rmse:.4f} h")
print(f"R²   : {time_r2:.4f}")


# =========================================================
# SAVE MODEL + SCALER
# =========================================================



# =========================================================
# SAVE TRAINED OBJECTS
# =========================================================
# Le modèle entraîné et le scaler sont sauvegardés afin d'être réutilisés
# dans le script de prédiction connecté à la base de données.
#
# Le même ordre des features doit être respecté pendant l'entraînement
# et pendant la prédiction réelle.

joblib.dump(model, MODEL_FILE)
joblib.dump(scaler, SCALER_FILE)


# =========================================================
# COMPARISON FILE
# =========================================================
comparison = pd.DataFrame({
    "timestamp": test_df["timestamp"].values,
    "bin_id": test_df["bin_id"].values,
    "cycle_id": test_df["cycle_id"].values,

    "real_fill_level_next": fill_true,
    "predicted_fill_level_next": fill_pred,
    "error_fill": np.abs(fill_true - fill_pred),

    "real_time_to_full_h": time_true,
    "predicted_time_to_full_h": time_pred,
    "error_time_h": np.abs(time_true - time_pred),
    "error_time_min": np.abs(time_true - time_pred) * 60,
})

comparison = comparison.round(2)

comparison.to_csv(COMPARISON_FILE, index=False)

print("\n===== COMPARISON HEAD =====")
print(comparison.head(30).to_string(index=False))

print("\n===== BEST PREDICTIONS =====")
print(
    comparison
    .sort_values(["error_fill", "error_time_h"])
    .head(10)
    .to_string(index=False)
)

print("\n===== WORST PREDICTIONS =====")
print(
    comparison
    .sort_values(["error_fill", "error_time_h"], ascending=False)
    .head(10)
    .to_string(index=False)
)


# =========================================================
# SAVE INFO
# =========================================================
print("\nSaved:")
print(MODEL_FILE)
print(SCALER_FILE)
print(COMPARISON_FILE)


# =========================================================
# SCATTER — FILL
# =========================================================
plt.figure(figsize=(7, 6))
plt.scatter(fill_true, fill_pred, alpha=0.6, s=20)
plt.plot([0, 100], [0, 100], "r--")
plt.title("Actual vs Predicted — Fill Level")
plt.xlabel("Actual")
plt.ylabel("Predicted")
plt.text(
    0.05,
    0.95,
    f"R² = {fill_r2:.4f}",
    transform=plt.gca().transAxes,
    verticalalignment="top",
    bbox=dict(boxstyle="round", facecolor="white", alpha=0.8)
)
plt.grid(True)
plt.show()


# =========================================================
# SCATTER — TIME
# =========================================================
plt.figure(figsize=(7, 6))
plt.scatter(time_true, time_pred, alpha=0.6, s=20)
plt.plot([0, TIME_MAX], [0, TIME_MAX], "r--")
plt.title("Actual vs Predicted — Time To Full")
plt.xlabel("Actual")
plt.ylabel("Predicted")
plt.text(
    0.05,
    0.95,
    f"R² = {time_r2:.4f}",
    transform=plt.gca().transAxes,
    verticalalignment="top",
    bbox=dict(boxstyle="round", facecolor="white", alpha=0.8)
)
plt.grid(True)
plt.show()


# =========================================================
# CURVES
# =========================================================
n = min(300, len(fill_true))

plt.figure(figsize=(12, 5))
plt.plot(fill_true[:n], label="Real Fill")
plt.plot(fill_pred[:n], label="Predicted Fill")
plt.legend()
plt.grid(True)
plt.title("Real vs Predicted — Fill")
plt.show()

plt.figure(figsize=(12, 5))
plt.plot(time_true[:n], label="Real Time")
plt.plot(time_pred[:n], label="Predicted Time")
plt.legend()
plt.grid(True)
plt.title("Real vs Predicted — Time To Full")
plt.show()


# =========================================================
# FEATURE IMPORTANCE
# =========================================================
print("\n===== FEATURE IMPORTANCE — FILL LEVEL MODEL =====")

fill_model = model.estimators_[0]

importance_fill = pd.DataFrame({
    "feature": features,
    "importance": fill_model.feature_importances_
}).sort_values("importance", ascending=False)

print(importance_fill.head(20).to_string(index=False))


print("\n===== FEATURE IMPORTANCE — TIME TO FULL MODEL =====")

time_model = model.estimators_[1]

importance_time = pd.DataFrame({
    "feature": features,
    "importance": time_model.feature_importances_
}).sort_values("importance", ascending=False)

print(importance_time.head(20).to_string(index=False))


print("\nDone.")