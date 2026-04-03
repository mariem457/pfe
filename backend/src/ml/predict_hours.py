from pathlib import Path
import sys
import joblib
import pandas as pd


def parse_bool_to_int(value):
    value = str(value).strip().lower()
    if value in ["true", "1", "yes", "y"]:
        return 1
    elif value in ["false", "0", "no", "n"]:
        return 0
    else:
        raise ValueError("collected must be true/false or 1/0")


if len(sys.argv) != 8:
    print("Usage: python predict_hours.py <hour> <fill_level> <fill_rate> <battery_level> <weight_kg> <rssi> <collected>")
    sys.exit(1)

hour = float(sys.argv[1])
fill_level = float(sys.argv[2])
fill_rate = float(sys.argv[3])
battery_level = float(sys.argv[4])
weight_kg = float(sys.argv[5])
rssi = float(sys.argv[6])
collected = parse_bool_to_int(sys.argv[7])

BASE_DIR = Path(__file__).resolve().parent
MODEL_PATH = BASE_DIR / "smart_bin_time_to_threshold_model.pkl"

model = joblib.load(MODEL_PATH)

data = pd.DataFrame([{
    "hour": hour,
    "fill_level": fill_level,
    "fill_rate": fill_rate,
    "battery_level": battery_level,
    "weight_kg": weight_kg,
    "rssi": rssi,
    "collected": collected
}])

predicted_hours = float(model.predict(data)[0])
predicted_hours = max(0.0, predicted_hours)

if predicted_hours <= 2:
    status = "URGENT"
elif predicted_hours <= 6:
    status = "COLLECT_SOON"
else:
    status = "NORMAL"

priority_score = max(0.0, min(1.0, 1 - (predicted_hours / 24.0)))
should_collect = predicted_hours <= 6

print(f"{predicted_hours:.2f},{status},{priority_score:.2f},{str(should_collect).lower()}")