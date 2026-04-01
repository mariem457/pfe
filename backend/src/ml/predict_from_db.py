import sys
import joblib
import pandas as pd
from pathlib import Path
def parse_bool_to_int(value):
    value = str(value).strip().lower()
    if value in ["true", "1", "yes", "y"]:
        return 1
    elif value in ["false", "0", "no", "n"]:
        return 0
    else:
        raise ValueError("collected must be true/false or 1/0")

if len(sys.argv) != 13:
    print("Usage: python predict_from_db.py <hour> <fill_level> <fill_rate> <battery_level> <weight_kg> <rssi> <collected> <fill_level_lag1> <fill_level_lag2> <fill_rate_lag1> <weight_kg_lag1> <rssi_lag1>")
    sys.exit(1)

hour = float(sys.argv[1])
fill_level = float(sys.argv[2])
fill_rate = float(sys.argv[3])
battery_level = float(sys.argv[4])
weight_kg = float(sys.argv[5])
rssi = float(sys.argv[6])
collected = parse_bool_to_int(sys.argv[7])

fill_level_lag1 = float(sys.argv[8])
fill_level_lag2 = float(sys.argv[9])
fill_rate_lag1 = float(sys.argv[10])
weight_kg_lag1 = float(sys.argv[11])
rssi_lag1 = float(sys.argv[12])

BASE_DIR = Path(__file__).resolve().parent
MODEL_PATH = BASE_DIR / "smart_bin_model_bd_lags.pkl"

model = joblib.load(MODEL_PATH)


data = pd.DataFrame([{
    "hour": hour,
    "fill_level": fill_level,
    "fill_rate": fill_rate,
    "battery_level": battery_level,
    "weight_kg": weight_kg,
    "rssi": rssi,
    "collected": collected,
    "fill_level_lag1": fill_level_lag1,
    "fill_level_lag2": fill_level_lag2,
    "fill_rate_lag1": fill_rate_lag1,
    "weight_kg_lag1": weight_kg_lag1,
    "rssi_lag1": rssi_lag1
}])

prediction = model.predict(data)[0]
prediction = max(0, min(100, prediction))

priority_score = round(prediction / 100.0, 2)

if prediction >= 90:
    alert_status = "URGENT"
elif prediction >= 80:
    alert_status = "COLLECT_SOON"
else:
    alert_status = "NORMAL"

should_collect = "true" if prediction >= 80 else "false"

print(f"{prediction:.2f},{alert_status},{priority_score:.2f},{should_collect}")
