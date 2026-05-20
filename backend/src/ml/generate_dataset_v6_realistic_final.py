"""
generate_dataset_v6_realistic.py
Dataset Smart Waste Management – Paris 15e – VERSION REALISTIC + REAL FUTURE TTF
"""

import numpy as np
import pandas as pd

# ─── CONFIG ───────────────────────────────────────────────────────────────────
BINS_FILE      = "trilib_paris15_zones.csv"
WEATHER_FILE   = "paris15_2025_hourly.csv"
ZONE_FILE      = "zone_activity_paris15.csv"
COMMERCES_FILE = "commerces_paris15_zones.csv"
OUT_FILE       = "final_dataset_paris15_v6_realistic.csv"

RNG_SEED = 42
np.random.seed(RNG_SEED)

# ─── CONSTANTES ───────────────────────────────────────────────────────────────
FILL_RATE_BASE = {
    "gris":  1.042,
    "jaune": 0.694,
    "blanc": 0.417,
}

ACTIVITY_FILL_FACTOR = {
    "High":   1.40,
    "Medium": 1.00,
    "Low":    0.60,
}

CAPACITY_LITERS = {
    "gris":  90.0,
    "jaune": 60.0,
    "blanc": 60.0,
}

DENSITY_KG_L = {
    "gris":  0.10,
    "jaune": 0.03,
    "blanc": 0.40,
}

CAPACITY_KG = {
    t: CAPACITY_LITERS[t] * DENSITY_KG_L[t]
    for t in CAPACITY_LITERS
}

# ─── LOAD DATA ────────────────────────────────────────────────────────────────
print("Chargement des données...")

bins_df    = pd.read_csv(BINS_FILE)
weather_df = pd.read_csv(WEATHER_FILE)
zone_df    = pd.read_csv(ZONE_FILE)
comm_df    = pd.read_csv(COMMERCES_FILE)

comm_counts = comm_df.groupby("zone").size().reset_index(name="commerce_count_real")
zone_df = zone_df.merge(comm_counts, on="zone", how="left")
zone_df["commerce_count_real"] = zone_df["commerce_count_real"].fillna(0).astype(int)

weather_df["time"]        = pd.to_datetime(weather_df["time"])
weather_df["hour"]        = weather_df["time"].dt.hour
weather_df["day_of_week"] = weather_df["time"].dt.dayofweek
weather_df["month"]       = weather_df["time"].dt.month
weather_df["is_weekend"]  = (weather_df["day_of_week"] >= 5).astype(int)
weather_df["prcp"]        = weather_df["prcp"].fillna(0)
weather_df = weather_df.sort_values("time").reset_index(drop=True)

bins_df["type_bin"] = bins_df["bin_id"].apply(
    lambda x: ["gris", "jaune", "blanc"][int(x) % 3]
)

print(f"Bacs: {len(bins_df)} | Météo: {len(weather_df)} heures | Zones: {len(zone_df)}")

# ─── PATTERNS TEMPORELS ───────────────────────────────────────────────────────
HOUR_PATTERN = {
     0: 0.20,  1: 0.15,  2: 0.12,  3: 0.10,  4: 0.12,  5: 0.35,
     6: 0.65,  7: 0.88,  8: 1.25,  9: 1.30, 10: 1.15, 11: 1.05,
    12: 1.22, 13: 1.18, 14: 1.02, 15: 0.98, 16: 1.08,
    17: 1.25, 18: 1.35, 19: 1.28, 20: 1.12, 21: 0.88,
    22: 0.60, 23: 0.38
}

MONTH_PATTERN = {
     1: 0.90,  2: 0.88,  3: 0.95,  4: 1.00,
     5: 1.05,  6: 1.10,  7: 1.15,  8: 1.08,
     9: 1.02, 10: 0.98, 11: 1.00, 12: 1.18
}

WEEKEND_FACTOR = {
    "gris":  1.08,
    "jaune": 1.05,
    "blanc": 1.18,
}

# Seuils de collecte plus réalistes par type de bac
COLLECTION_THRESHOLD = {
    "gris": 80,
    "jaune": 75,
    "blanc": 70,
}

# Interaction weekend × activité de zone
ZONE_WEEKEND_ACTIVITY_FACTOR = {
    "High":   1.25,
    "Medium": 1.10,
    "Low":    0.95,
}

# Heures de pic réalistes
PEAK_HOURS_FACTOR = {
    "morning": 1.15,
    "lunch":   1.20,
    "evening": 1.30,
    "night":   0.70,
}

# probabilities fixed: أقل chaos
EVENT_SPIKE_PROB = {
    "High":   0.006,
    "Medium": 0.004,
    "Low":    0.002,
}

RANDOM_SENSOR_DRIFT_PROB = 0.0015
RANDOM_BATTERY_DROP_PROB = 0.0004
RANDOM_NETWORK_LOSS_PROB = 0.001

COLLECTION_SCHEDULE = {
    "gris":  {"days": [0, 2, 4], "hour": 6},
    "jaune": {"days": [1, 4],    "hour": 7},
    "blanc": {"days": [2, 5],    "hour": 8},
}

def should_collect(type_bin, dow, hour, fill_level):
    sched = COLLECTION_SCHEDULE[type_bin]
    threshold = COLLECTION_THRESHOLD[type_bin]

    # Collecte planifiée seulement si le bac est suffisamment rempli
    if (dow in sched["days"]) and (hour == sched["hour"]) and fill_level >= threshold:
        return True

    # Collecte urgente si saturation proche
    if fill_level >= 92:
        return True

    return False

# ─── SIMULATION ───────────────────────────────────────────────────────────────
print("\nDébut simulation fixed...")

all_records = []

for _, bin_row in bins_df.iterrows():
    bin_id   = int(bin_row["bin_id"])
    address  = bin_row["address"]
    arrond   = int(bin_row["Arrondissement"])
    zone     = bin_row["zone"]
    lat      = float(bin_row["latitude"])
    lon      = float(bin_row["longitude"])
    type_bin = bin_row["type_bin"]

    z_info  = zone_df[zone_df["zone"] == zone].iloc[0]
    pop_est = int(z_info["population_estimee"])
    surf    = float(z_info["surface_km2"])
    density = int(z_info["density_hab_km2"])
    n_comm  = int(z_info["commerce_count_real"])
    act_lvl = z_info["activity_level"]

    fill_rate_b = FILL_RATE_BASE[type_bin] * ACTIVITY_FILL_FACTOR[act_lvl]
    capacity_kg = CAPACITY_KG[type_bin]

    max_comm = 52.0
    comm_factor = 1.0 + (n_comm / max_comm) * 0.35

    fill = float(np.random.uniform(8, 60))
    battery = float(np.random.uniform(82, 100))
    hours_since_coll = int(np.random.randint(5, 120))

    bin_usage_factor = float(np.random.lognormal(mean=0.0, sigma=0.14))
    bin_sensor_noise = float(np.random.uniform(0.30, 0.90))
    bin_rssi_offset = float(np.random.normal(0.0, 2.2))
    bin_battery_age = float(np.random.uniform(0.85, 1.25))
    bin_event_sensitivity = float(np.random.uniform(0.7, 1.25))

    # Mémoire temporelle: évite que le fill_rate saute brutalement d'une heure à l'autre
    previous_fill_rate = fill_rate_b

    for _, w in weather_df.iterrows():
        hour_   = int(w["hour"])
        dow     = int(w["day_of_week"])
        month_  = int(w["month"])
        is_wknd = int(w["is_weekend"])
        temp    = float(w["temp"])
        rhum    = float(w["rhum"])
        prcp    = float(w["prcp"])
        wspd    = float(w["wspd"])
        pres    = float(w["pres"])
        coco    = float(w["coco"])

        # ── FILL RATE LOGIQUE ────────────────────────────────────────────────
        h_factor = HOUR_PATTERN[hour_]
        m_factor = MONTH_PATTERN[month_]
        wknd_factor = WEEKEND_FACTOR[type_bin] if is_wknd else 1.0
        zone_weekend_factor = ZONE_WEEKEND_ACTIVITY_FACTOR[act_lvl] if is_wknd else 1.0

        if 7 <= hour_ <= 9:
            peak_factor = PEAK_HOURS_FACTOR["morning"]
        elif 12 <= hour_ <= 14:
            peak_factor = PEAK_HOURS_FACTOR["lunch"]
        elif 18 <= hour_ <= 21:
            peak_factor = PEAK_HOURS_FACTOR["evening"]
        elif 0 <= hour_ <= 5:
            peak_factor = PEAK_HOURS_FACTOR["night"]
        else:
            peak_factor = 1.0

        rain_factor = max(0.70, 1.0 - 0.035 * prcp)
        temp_factor = 1.0 + max(0.0, (temp - 15.0)) * 0.005
        post_coll_ramp = min(1.0, 0.35 + hours_since_coll / 40.0)

        day_of_month = int(w["time"].day)
        payday_factor = 1.07 if day_of_month in [1, 2, 28, 29, 30, 31] else 1.0
        december_event_factor = 1.10 if month_ == 12 else 1.0

        event_factor = 1.0
        event_type = "none"
        event_probability = EVENT_SPIKE_PROB.get(act_lvl, 0.003)

        if np.random.random() < event_probability and 7 <= hour_ <= 22:
            event_factor = float(np.random.uniform(1.05, 1.35) * bin_event_sensitivity)
            event_type = "local_activity_spike"

        effective_rate = (
            fill_rate_b
            * comm_factor
            * h_factor
            * m_factor
            * wknd_factor
            * zone_weekend_factor
            * peak_factor
            * rain_factor
            * temp_factor
            * post_coll_ramp
            * payday_factor
            * december_event_factor
            * bin_usage_factor
            * event_factor
        )

        # noise fixed: ما عادش قوي برشا
        if np.random.random() < 0.015:
            noise_mult = np.random.lognormal(mean=0.0, sigma=0.12)
        else:
            noise_mult = np.random.normal(1.0, 0.05)

        raw_rate = max(0.01, effective_rate * noise_mult)

        # Mémoire temporelle / smoothing du taux de remplissage
        fill_rate_true = 0.75 * previous_fill_rate + 0.25 * raw_rate
        previous_fill_rate = fill_rate_true

        # ── UPDATE TRUE FILL ─────────────────────────────────────────────────
        fill = min(100.0, fill + fill_rate_true)
        hours_since_coll += 1

        # ── COLLECTION ───────────────────────────────────────────────────────
        collected = 0
        if should_collect(type_bin, dow, hour_, fill):
            collected = 1
            fill = float(np.random.uniform(2, 5))
            hours_since_coll = 0

        recorded_fill_rate = round(fill_rate_true, 4)

        # ── BATTERIE ─────────────────────────────────────────────────────────
        batt_drop = 0.0042 + 0.00018 * max(0.0, wspd - 5.0)
        batt_drop *= np.random.normal(1.0, 0.10) * bin_battery_age

        if np.random.random() < RANDOM_BATTERY_DROP_PROB:
            batt_drop += float(np.random.uniform(0.5, 2.0))

        if battery < 12 and np.random.random() < 0.020:
            battery = float(np.random.uniform(88, 100))
        else:
            battery = float(np.clip(battery - batt_drop, 3.0, 100.0))

        # ── RSSI ─────────────────────────────────────────────────────────────
        base_rssi = {"High": -74, "Medium": -68, "Low": -62}.get(act_lvl, -68)
        wind_rssi = -min(4.0, wspd / 12.0)
        rain_rssi = -min(3.0, prcp * 0.45)

        rssi = float(
            np.clip(
                base_rssi
                + bin_rssi_offset
                + wind_rssi
                + rain_rssi
                + np.random.normal(0, 2.4),
                -98,
                -45
            )
        )

        if np.random.random() < RANDOM_NETWORK_LOSS_PROB:
            rssi = -120.0

        # ── SENSOR NOISE FIXED ───────────────────────────────────────────────
        # IMPORTANT:
        # fill = vrai état interne du bac
        # fill_sensor = valeur mesurée par le capteur
        # Le bruit capteur ne doit jamais modifier fill.

        sensor_noise = np.random.normal(0, 0.35 * bin_sensor_noise)

        if np.random.random() < RANDOM_SENSOR_DRIFT_PROB:
            sensor_noise += np.random.normal(0, 1.2)

        fill_sensor = float(np.clip(fill + sensor_noise, 0, 100))

        # ── POIDS basé sur TRUE fill ─────────────────────────────────────────
        fill_pct = fill / 100.0
        weight_kg = round(capacity_kg * (fill_pct ** 0.88), 3)

        # ── ANOMALIES MESURE ────────────────────────────────────────────────
        r = np.random.random()

        if r < 0.0015:
            fill_rec = float(np.clip(fill_sensor + np.random.normal(0, 5), 0, 100))
            anomaly = "sensor_spike"

        elif r < 0.002 or rssi <= -115:
            fill_rec = np.nan
            anomaly = "signal_loss"

        elif battery < 15 and r < 0.008:
            fill_rec = float(np.clip(fill_sensor + np.random.normal(0, 3), 0, 100))
            anomaly = "battery_warning"

        elif event_type != "none" and r < 0.020:
            fill_rec = round(fill_sensor, 2)
            anomaly = event_type

        else:
            fill_rec = round(fill_sensor, 2)
            anomaly = "none"

        all_records.append({
            "bin_id": bin_id,
            "address": address,
            "Arrondissement": arrond,
            "zone": zone,
            "type_bin": type_bin,
            "latitude": lat,
            "longitude": lon,
            "population_estimee": pop_est,
            "surface_km2": surf,
            "density_hab_km2": density,
            "commerce_count": n_comm,
            "activity_level": act_lvl,
            "time": w["time"],
            "hour": hour_,
            "day_of_week": dow,
            "month": month_,
            "is_weekend": is_wknd,
            "temp": round(temp, 1),
            "rhum": round(rhum, 1),
            "prcp": round(prcp, 2),
            "wspd": round(wspd, 1),
            "pres": round(pres, 1),
            "coco": int(coco),
            "fill_level": fill_rec,
            "fill_rate": recorded_fill_rate,
            "battery_level": round(battery, 2),
            "weight_kg": weight_kg,
            "rssi": round(rssi, 1),
            "collected": collected,
            "hours_since_collection": hours_since_coll,
            "is_collection_hour": int((dow in COLLECTION_SCHEDULE[type_bin]["days"]) and (hour_ == COLLECTION_SCHEDULE[type_bin]["hour"])),
            "anomaly_type": anomaly,
        })

    print(
        f"✓ Bac {bin_id:5d} | {zone:15s} | {type_bin:5s} | {act_lvl:6s} "
        f"| fill_rate_base={fill_rate_b:.3f}%/h"
    )

# ─── ASSEMBLAGE ───────────────────────────────────────────────────────────────
print("\nAssemblage dataframe final...")

df = pd.DataFrame(all_records)
df = df.sort_values(["bin_id", "time"]).reset_index(drop=True)

df["has_anomaly"] = (df["anomaly_type"] != "none").astype(int)

# ─── REAL FUTURE TIME_TO_FULL ────────────────────────────────────────────────
# Objectif:
# time_to_full ne sera plus calculé par formule.
# Il sera extrait du futur réel de chaque bac:
# "dans combien d'heures ce bac atteint 90% ?"

CRITICAL_FILL_LEVEL = 90
MAX_TIME_TO_FULL_HOURS = 48

def compute_real_future_time_to_full(group):
    group = group.sort_values("time").copy()
    fill_values = group["fill_level"].values
    times = group["time"].values

    result = []

    for i in range(len(group)):
        current_fill = fill_values[i]

        # Si valeur manquante, on ne sait pas calculer
        if pd.isna(current_fill):
            result.append(np.nan)
            continue

        # Si déjà critique
        if current_fill >= CRITICAL_FILL_LEVEL:
            result.append(0.0)
            continue

        found = False

        # On cherche dans les 48 prochaines heures
        for j in range(i + 1, min(i + MAX_TIME_TO_FULL_HOURS + 1, len(group))):
            future_fill = fill_values[j]

            if pd.isna(future_fill):
                continue

            if future_fill >= CRITICAL_FILL_LEVEL:
                delta_hours = (
                    pd.to_datetime(times[j]) - pd.to_datetime(times[i])
                ).total_seconds() / 3600.0

                result.append(delta_hours)
                found = True
                break

        # Si le bac n'atteint pas 90% dans 48h
        # on met 48h = non urgent dans l'horizon opérationnel
        if not found:
            result.append(float(MAX_TIME_TO_FULL_HOURS))

    group["time_to_full"] = result
    return group


df = (
    df.groupby("bin_id", group_keys=False)
      .apply(compute_real_future_time_to_full)
      .reset_index(drop=True)
)

# Smoothing robuste du target time_to_full:
# median rolling centrée réduit les sauts brutaux causés par anomalies/collectes.
df["time_to_full"] = (
    df.groupby("bin_id")["time_to_full"]
      .transform(lambda x: x.rolling(5, min_periods=1, center=True).median())
)
df["time_to_full"] = df["time_to_full"].clip(0, MAX_TIME_TO_FULL_HOURS)

# Features supplémentaires pour aider le modèle LSTM
df["fill_delta_1h"] = df.groupby("bin_id")["fill_level"].diff(1)
df["fill_delta_3h"] = df.groupby("bin_id")["fill_level"].diff(3)
df["fill_delta_6h"] = df.groupby("bin_id")["fill_level"].diff(6)

for col in ["fill_delta_1h", "fill_delta_3h", "fill_delta_6h"]:
    df[col] = df.groupby("bin_id")[col].transform(lambda x: x.ffill().bfill()).fillna(0)

# ─── VALIDATION ───────────────────────────────────────────────────────────────
df_check = df[df["anomaly_type"] == "none"].copy()
df_check["fill_level_next"] = df_check.groupby("bin_id")["fill_level"].shift(-1)
df_check = df_check.dropna(subset=["fill_level_next"])

corr_fl = df_check["fill_level"].corr(df_check["fill_level_next"])
corr_fr = df_check["fill_rate"].corr(
    df_check["fill_level_next"] - df_check["fill_level"]
)

print("\n" + "=" * 60)
print("VALIDATION DATASET FIXED")
print("=" * 60)
print(f"Rows total:          {len(df):,}")
print(f"Bacs:                {df['bin_id'].nunique()}")
print(f"Période:             {df['time'].min()} → {df['time'].max()}")
print(f"Anomalies:           {df['anomaly_type'].value_counts().to_dict()}")
print(f"Collections totales: {df['collected'].sum():,}")
print(f"Fill level moyen:    {df['fill_level'].mean():.2f}%")
print(f"Fill level max:      {df['fill_level'].max():.2f}%")
print(f"Battery min/max:     {df['battery_level'].min():.2f}% / {df['battery_level'].max():.2f}%")
print(f"RSSI min/max:        {df['rssi'].min():.1f} / {df['rssi'].max():.1f} dBm")
print(f"NaN fill_level:      {df['fill_level'].isna().sum():,}")
print()
print("[LEAKAGE / LOGIC CHECK]")
print(f"Corr fill_level ↔ fill_level_next: {corr_fl:.4f}")
print(f"Corr fill_rate ↔ delta futur:      {corr_fr:.4f}")
print()
print("[CALIBRATION]")
for t in ["gris", "jaune", "blanc"]:
    sub = df_check[df_check["type_bin"] == t]
    print(
        f"{t:5s}: fill_rate moyen = {sub['fill_rate'].mean():.3f}%/h | "
        f"capacity = {CAPACITY_KG[t]:.1f}kg | "
        f"poids moyen = {sub['weight_kg'].mean():.2f}kg"
    )
print("=" * 60)

# ─── SAVE ────────────────────────────────────────────────────────────────────
df.to_csv(OUT_FILE, index=False, encoding="utf-8")
print(f"\n✅ Dataset sauvegardé : {OUT_FILE}")
