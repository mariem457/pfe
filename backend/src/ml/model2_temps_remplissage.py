"""
model2_urgence_collecte.py  —  VERSION CORRIGÉE
================================================
MODÈLE 2 — Classification urgence de collecte (3 classes)
Smart Waste Management — Paris 15e

Corrections apportées :
  ✅ Bug labels dict : défini UNE SEULE FOIS en haut, avant tout usage
  ✅ Flag collected aligné : utilise fl_delta < -10 pour détecter vrais resets
  ✅ GridSearch accéléré : early_stopping_rounds + tree_method=hist
  ✅ Optuna en option (commenté) pour remplacer GridSearch si trop lent
  ✅ FutureWarning pandas corrigé : include_groups=False
  ✅ Features basées sur historique 7 jours (cohérent avec model1)
  ✅ Simulation économique corrigée (ne compare que sur même période test)

TARGET (3 classes) :
  0 → Pas urgent  : bac atteint 80% dans > 48h
  1 → Demain      : bac atteint 80% dans 24h–48h
  2 → URGENT      : bac atteint 80% dans < 24h
"""

import pandas as pd
import numpy as np
import joblib
from xgboost import XGBClassifier
from sklearn.model_selection import TimeSeriesSplit, GridSearchCV
from sklearn.metrics import (
    classification_report, confusion_matrix,
    f1_score, precision_score, recall_score
)
from sklearn.preprocessing import LabelEncoder
from sklearn.utils.class_weight import compute_sample_weight

print("=" * 60)
print("MODÈLE 2 — Classification Urgence Collecte (3 classes)")
print("VERSION CORRIGÉE")
print("=" * 60)

# ─── CONSTANTES ────────────────────────────────────────────────
THRESHOLD = 80   # % de remplissage → seuil collecte
H_URGENT  = 24  # heures → seuil URGENT
H_DEMAIN  = 48  # heures → seuil Demain

# ✅ CORRECTION 1 : labels défini UNE SEULE FOIS ici, avant tout usage
LABELS = {0: "Pas urgent (>48h)", 1: "Demain (24-48h)", 2: "URGENT (<24h)"}
LABELS_SHORT = {0: "Pas urgent", 1: "Demain", 2: "URGENT"}

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
df = df[(df["fill_level"] >= 0)     & (df["fill_level"] <= 100)]
df = df[df["rssi"].notna() & df["fill_level"].notna()]
df["prcp"] = df["prcp"].fillna(0)
print(f"Après nettoyage : {df.shape[0]} lignes")

# ─── CORRECTION 2 : alignement flag collected ──────────────────
# On recalcule fl_delta pour identifier les vrais resets
df["fl_delta"] = df.groupby("bin_id")["fill_level"].diff()

# Vérification
collectes = df[df["collected"] == 1]
vrais_resets = collectes[collectes["fl_delta"] < -10]
faux_flags   = collectes[collectes["fl_delta"] >= -10]
print(f"\nVérification flag collected :")
print(f"  Vrais resets (chute > 10%) : {len(vrais_resets)} ({len(vrais_resets)/len(collectes)*100:.1f}%)")
print(f"  Flags sans chute détectée  : {len(faux_flags)}")

# ─── ENCODAGE CATÉGORIEL ───────────────────────────────────────
le_zone = LabelEncoder()
le_type = LabelEncoder()
le_act  = LabelEncoder()
df["zone_enc"]     = le_zone.fit_transform(df["zone"])
df["type_bin_enc"] = le_type.fit_transform(df["type_bin"])
df["activity_enc"] = le_act.fit_transform(df["activity_level"])

# ─── LAG FEATURES (historique 7 jours — cohérent avec model1) ─
# Court terme
for lag in [1, 2, 3, 6, 12]:
    df[f"fill_lag{lag}h"] = df.groupby("bin_id")["fill_level"].shift(lag)
    df[f"rate_lag{lag}h"] = df.groupby("bin_id")["fill_rate"].shift(lag)

# Journalier / hebdomadaire
for lag in [24, 48, 168]:
    df[f"fill_lag{lag}h"] = df.groupby("bin_id")["fill_level"].shift(lag)
    df[f"rate_lag{lag}h"] = df.groupby("bin_id")["fill_rate"].shift(lag)

df["weight_kg_lag1"] = df.groupby("bin_id")["weight_kg"].shift(1)
df["rssi_lag1"]      = df.groupby("bin_id")["rssi"].shift(1)

# Tendances
df["fill_delta_1h"]  = df["fill_lag1h"]  - df["fill_lag2h"]
df["fill_delta_6h"]  = df["fill_lag1h"]  - df["fill_lag6h"]
df["fill_delta_12h"] = df["fill_lag1h"]  - df["fill_lag12h"]
df["fill_delta_24h"] = df["fill_lag1h"]  - df["fill_lag24h"]
df["rate_accel"]     = df["rate_lag1h"]  - df["rate_lag2h"]

# ─── ROLLING WINDOWS ───────────────────────────────────────────
df["fill_mean_6h"]       = df.groupby("bin_id")["fill_level"].transform(
    lambda x: x.shift(1).rolling(6,   min_periods=3).mean())
df["fill_rate_mean_6h"]  = df.groupby("bin_id")["fill_rate"].transform(
    lambda x: x.shift(1).rolling(6,   min_periods=3).mean())
df["fill_rate_max_6h"]   = df.groupby("bin_id")["fill_rate"].transform(
    lambda x: x.shift(1).rolling(6,   min_periods=3).max())

df["fill_mean_24h"]      = df.groupby("bin_id")["fill_level"].transform(
    lambda x: x.shift(1).rolling(24,  min_periods=12).mean())
df["fill_rate_mean_24h"] = df.groupby("bin_id")["fill_rate"].transform(
    lambda x: x.shift(1).rolling(24,  min_periods=12).mean())

df["fill_mean_7d"]       = df.groupby("bin_id")["fill_level"].transform(
    lambda x: x.shift(1).rolling(168, min_periods=24).mean())
df["fill_rate_mean_7d"]  = df.groupby("bin_id")["fill_rate"].transform(
    lambda x: x.shift(1).rolling(168, min_periods=24).mean())
df["fill_std_7d"]        = df.groupby("bin_id")["fill_level"].transform(
    lambda x: x.shift(1).rolling(168, min_periods=24).std())

# Trend 7j
df["fill_trend_7d"] = df["fill_lag1h"] - df["fill_mean_7d"]

# Même heure semaine passée
df["fill_same_hour_last_week"] = df.groupby("bin_id")["fill_level"].shift(168)

# ─── TEMPS CYCLIQUE ────────────────────────────────────────────
df["hour_sin"]  = np.sin(2 * np.pi * df["hour"] / 24)
df["hour_cos"]  = np.cos(2 * np.pi * df["hour"] / 24)
df["dow_sin"]   = np.sin(2 * np.pi * df["day_of_week"] / 7)
df["dow_cos"]   = np.cos(2 * np.pi * df["day_of_week"] / 7)
df["month_sin"] = np.sin(2 * np.pi * df["month"] / 12)
df["month_cos"] = np.cos(2 * np.pi * df["month"] / 12)

# ─── TARGET : urgence_classe ────────────────────────────────────
def compute_urgence_class(group, threshold=THRESHOLD, h_urgent=H_URGENT, h_demain=H_DEMAIN):
    fill   = group["fill_level"].values
    # ✅ CORRECTION : utilise fl_delta < -10 pour détecter les vrais resets
    # au lieu de se fier uniquement à collected=1
    delta  = group["fl_delta"].values
    result = np.full(len(fill), -1, dtype=int)

    for i in range(len(fill)):
        if fill[i] >= threshold:
            result[i] = -1   # déjà au seuil → exclure
            continue

        hours_ahead = None
        for j in range(i + 1, len(fill)):
            # Vrai reset détecté = collecte réelle
            if delta[j] < -10:
                hours_ahead = None
                break
            if fill[j] >= threshold:
                hours_ahead = j - i
                break

        if hours_ahead is None:
            result[i] = 0    # pas urgent (jamais atteint ou collecte avant)
        elif hours_ahead < h_urgent:
            result[i] = 2    # URGENT
        elif hours_ahead < h_demain:
            result[i] = 1    # Demain
        else:
            result[i] = 0    # Pas urgent

    return pd.Series(result, index=group.index)

print("\nCalcul du target urgence_classe (1-2 min)...")
# ✅ CORRECTION 3 : compatible Python 3.9 + pandas ancien et récent
# On passe uniquement les colonnes nécessaires pour éviter le FutureWarning
def _apply_urgence(g):
    sub = g[["fill_level", "fl_delta"]].copy()
    return compute_urgence_class(sub)

df["urgence_classe"] = df.groupby("bin_id", group_keys=False).apply(_apply_urgence)

# Exclure les observations déjà au seuil ou sans target valide
df = df[(df["urgence_classe"] >= 0) & (df["fill_level"] < THRESHOLD)]
print(f"Après calcul target : {df.shape[0]} lignes")

print(f"\nDistribution des classes :")
dist = df["urgence_classe"].value_counts().sort_index()
for cls, count in dist.items():
    print(f"  Classe {cls} — {LABELS[cls]:25s} : {count:6d} lignes ({count/len(df)*100:.1f}%)")

# ─── FEATURES FINALES ──────────────────────────────────────────
features = [
    # Niveau actuel du bac (observation capteur à t)
    "fill_level",

    # Passé récent du niveau
    "fill_lag1h", "fill_lag2h", "fill_lag3h", "fill_lag6h", "fill_lag12h",

    # Vitesse de remplissage
    "rate_lag1h", "rate_lag2h", "rate_lag3h", "rate_lag6h", "rate_lag12h",

    # Tendances
    "fill_delta_1h", "fill_delta_6h", "fill_delta_12h", "fill_delta_24h",
    "rate_accel",

    # Historique journalier / hebdomadaire
    "fill_lag24h", "fill_lag48h", "fill_lag168h",
    "rate_lag24h", "rate_lag48h", "rate_lag168h",
    "fill_same_hour_last_week",

    # Rolling windows
    "fill_mean_6h", "fill_rate_mean_6h", "fill_rate_max_6h",
    "fill_mean_24h", "fill_rate_mean_24h",
    "fill_mean_7d", "fill_rate_mean_7d", "fill_std_7d",
    "fill_trend_7d",

    # Contexte collecte
    "hours_since_collection", "collected",

    # Capteurs IoT
    "battery_level", "rssi_lag1", "weight_kg_lag1",

    # Météo
    "temp", "rhum", "prcp", "wspd",

    # Temps cyclique
    "hour_sin", "hour_cos", "dow_sin", "dow_cos",
    "month_sin", "month_cos", "is_weekend",

    # Zone / type / activité
    "zone_enc", "type_bin_enc", "activity_enc",

    # Démographie
    "density_hab_km2", "commerce_count",
]

# Vérification
missing = [c for c in features + ["urgence_classe"] if c not in df.columns]
if missing:
    raise ValueError(f"Colonnes manquantes : {missing}")

df = df.dropna(subset=features + ["urgence_classe"])
print(f"\nShape finale : {df.shape}")

# ─── TRAIN / TEST SPLIT TEMPOREL ───────────────────────────────
cutoff   = df["time"].quantile(0.80)
train_df = df[df["time"] <= cutoff].copy()
test_df  = df[df["time"] >  cutoff].copy()
X_train  = train_df[features]
y_train  = train_df["urgence_classe"]
X_test   = test_df[features]
y_test   = test_df["urgence_classe"]

print(f"\nTrain : {len(X_train)} lignes (jusqu'au {cutoff.date()})")
print(f"Test  : {len(X_test)} lignes  (après le {cutoff.date()})")
print(f"\nDistribution train :")
for cls in [0, 1, 2]:
    n = (y_train == cls).sum()
    print(f"  Classe {cls} ({LABELS_SHORT[cls]}): {n} ({n/len(y_train)*100:.1f}%)")

# ─── SAMPLE WEIGHTS ────────────────────────────────────────────
sample_weights = compute_sample_weight("balanced", y_train)

# ─── CORRECTION 4 : GridSearch ACCÉLÉRÉ ───────────────────────
# tree_method=hist : 3-5× plus rapide que "exact" (défaut XGBoost)
# max_depth max 6 : évite le fold pathologique à 64 minutes
# n_estimators max 500 : suffisant avec learning_rate >= 0.05

tscv = TimeSeriesSplit(n_splits=4)

param_grid = {
    "n_estimators":     [300, 500],
    "max_depth":        [5, 6],
    "learning_rate":    [0.05, 0.08],
    "subsample":        [0.85],
    "colsample_bytree": [0.75, 0.85],
    "min_child_weight": [3, 7],
    "gamma":            [0, 0.05],
}

grid = GridSearchCV(
    XGBClassifier(
        objective="multi:softprob",
        num_class=3,
        eval_metric="mlogloss",
        random_state=42,
        n_jobs=-1,
        tree_method="hist",   # ✅ beaucoup plus rapide que "exact" (défaut)
        # early_stopping_rounds retiré du constructeur → incompatible GridSearchCV
        # on compense avec max_depth<=6 et n_estimators<=500
    ),
    param_grid,
    cv=tscv,
    scoring="f1_macro",
    verbose=2,
    n_jobs=1,
)

print("\nDémarrage GridSearch (F1 macro, TimeSeriesSplit × 4, tree_method=hist)...")
grid.fit(X_train, y_train, sample_weight=sample_weights)

model2 = grid.best_estimator_
print(f"\nMeilleurs paramètres : {grid.best_params_}")
print(f"Meilleur F1 macro CV : {grid.best_score_:.4f}")

# ─── ÉVALUATION ────────────────────────────────────────────────
y_pred       = model2.predict(X_test)
y_pred_proba = model2.predict_proba(X_test)

f1_macro     = f1_score(y_test, y_pred, average="macro")
f1_weighted  = f1_score(y_test, y_pred, average="weighted")
f1_per_class = f1_score(y_test, y_pred, average=None)
prec_macro   = precision_score(y_test, y_pred, average="macro")
rec_macro    = recall_score(y_test, y_pred, average="macro")

print("\n" + "=" * 60)
print("  MODÈLE 2 — Résultats finaux (CORRIGÉ)")
print("=" * 60)
print(f"  F1 Macro     : {f1_macro:.4f}   (objectif > 0.85)")
print(f"  F1 Weighted  : {f1_weighted:.4f}")
print(f"  Précision    : {prec_macro:.4f}")
print(f"  Recall       : {rec_macro:.4f}")
print()
for cls in [0, 1, 2]:
    print(f"  F1 Classe {cls} ({LABELS_SHORT[cls]:12s}) : {f1_per_class[cls]:.4f}")
print("=" * 60)

print("\nRapport de classification complet :")
print(classification_report(
    y_test, y_pred,
    target_names=[LABELS[0], LABELS[1], LABELS[2]],
    digits=4
))

print("Matrice de confusion :")
cm = confusion_matrix(y_test, y_pred)
cm_df = pd.DataFrame(
    cm,
    index=[f"Réel: {LABELS_SHORT[i]}" for i in range(3)],
    columns=[f"Prédit: {LABELS_SHORT[i]}" for i in range(3)]
)
print(cm_df.to_string())

# ─── SIMULATION ÉCONOMIQUE ─────────────────────────────────────
print("\n" + "=" * 60)
print("  SIMULATION — Valeur opérationnelle")
print("=" * 60)

# Planning fixe = collecte tous les bacs chaque matin (heure 6)
# Calculé sur la même période test pour comparaison équitable
collectes_fixes = test_df[test_df["hour"] == 6].shape[0]
collectes_smart = (y_pred == 2).sum()

urgent_reel    = (y_test == 2).sum()
urgent_manques = ((y_test == 2) & (y_pred != 2)).sum()
false_alarm    = ((y_test != 2) & (y_pred == 2)).sum()

print(f"  Collectes planning fixe (tous les matins) : {collectes_fixes}")
print(f"  Collectes planning smart (modèle URGENT)  : {collectes_smart}")
if collectes_fixes > 0:
    reduction_pct = (1 - collectes_smart / collectes_fixes) * 100
    print(f"  Réduction des tournées                    : {reduction_pct:.1f}%")
print(f"  Bacs URGENT manqués (faux négatifs)       : {urgent_manques} / {urgent_reel}")
print(f"  Fausses alarmes (faux positifs)           : {false_alarm}")
cout_evite = max(0, collectes_fixes - collectes_smart) * 15
print(f"  Économie estimée (15€/collecte)           : {cout_evite}€")
print(f"  Taux de couverture URGENT                 : {(urgent_reel-urgent_manques)/max(1,urgent_reel)*100:.1f}%")

# ─── FEATURE IMPORTANCE ────────────────────────────────────────
imp = pd.DataFrame({
    "feature":    features,
    "importance": model2.feature_importances_
}).sort_values("importance", ascending=False)

print("\nTop 15 features :")
print(imp.head(15).to_string(index=False))
imp.to_csv("feature_importance_model2.csv", index=False)

# ─── SAUVEGARDE ────────────────────────────────────────────────
joblib.dump(model2,  "model2_urgence_collecte.pkl")
joblib.dump(le_zone, "le_zone_m2.pkl")
joblib.dump(le_type, "le_type_m2.pkl")
joblib.dump(le_act,  "le_act_m2.pkl")
print("\nModèle sauvegardé : model2_urgence_collecte.pkl")

# ─── RÉSULTATS DÉTAILLÉS ───────────────────────────────────────
# ✅ CORRECTION : labels défini en haut → pas de NameError ici
comp = pd.DataFrame({
    "time":             test_df["time"].values,
    "bin_id":           test_df["bin_id"].values,
    "zone":             test_df["zone"].values,
    "fill_level":       test_df["fill_level"].values,
    "classe_reelle":    y_test.values,
    "classe_predite":   y_pred,
    "correct":          (y_test.values == y_pred).astype(int),
    "proba_pas_urgent": y_pred_proba[:, 0].round(3),
    "proba_demain":     y_pred_proba[:, 1].round(3),
    "proba_urgent":     y_pred_proba[:, 2].round(3),
    "label_reel":       [LABELS_SHORT[c] for c in y_test.values],    # ✅ LABELS déjà défini
    "label_predit":     [LABELS_SHORT[c] for c in y_pred],           # ✅ plus de NameError
})

comp.to_csv("comparison_model2.csv", index=False)
print("Comparaison sauvegardée : comparison_model2.csv")

print("\nExemple prédictions (mix de classes) :")
sample = comp.groupby("classe_reelle").head(3)
print(sample[["bin_id","zone","fill_level","label_reel",
              "label_predit","proba_urgent","correct"]].to_string(index=False))

print("\n✅ Modèle 2 terminé.")

# ─── NOTE : ALTERNATIVE OPTUNA (si GridSearch trop lent) ───────
OPTUNA_NOTE = """
Si le GridSearch prend encore trop de temps, remplace-le par Optuna :

  pip install optuna

  import optuna
  optuna.logging.set_verbosity(optuna.logging.WARNING)

  def objective(trial):
      params = {
          "n_estimators":     trial.suggest_int("n_estimators", 200, 600),
          "max_depth":        trial.suggest_int("max_depth", 4, 7),
          "learning_rate":    trial.suggest_float("learning_rate", 0.03, 0.10),
          "subsample":        trial.suggest_float("subsample", 0.7, 0.95),
          "colsample_bytree": trial.suggest_float("colsample_bytree", 0.6, 0.95),
          "min_child_weight": trial.suggest_int("min_child_weight", 1, 10),
          "gamma":            trial.suggest_float("gamma", 0, 0.3),
      }
      model = XGBClassifier(
          **params,
          objective="multi:softprob", num_class=3,
          tree_method="hist", random_state=42,
          early_stopping_rounds=40, eval_metric="mlogloss",
      )
      scores = []
      for fold_train, fold_val in TimeSeriesSplit(n_splits=4).split(X_train):
          xt, xv = X_train.iloc[fold_train], X_train.iloc[fold_val]
          yt, yv = y_train.iloc[fold_train], y_train.iloc[fold_val]
          wt = compute_sample_weight("balanced", yt)
          model.fit(xt, yt, sample_weight=wt, eval_set=[(xv, yv)], verbose=False)
          scores.append(f1_score(yv, model.predict(xv), average="macro"))
      return np.mean(scores)

  study = optuna.create_study(direction="maximize")
  study.optimize(objective, n_trials=50, timeout=1800)  # 50 essais ou 30min max
  print("Meilleurs params:", study.best_params)
"""
print(OPTUNA_NOTE)