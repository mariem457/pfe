import sys
import os
import joblib
import pandas as pd

base_dir = os.path.dirname(os.path.abspath(__file__))
model_path = os.path.join(base_dir, "smart_bin_model.pkl")

if len(sys.argv) != 8:
    print("Usage: python predict_features.py <hour> <day> <fill_level> <fill_rate> <battery_level> <weight_kg> <rssi>")
    sys.exit(1)

hour = float(sys.argv[1])
day = float(sys.argv[2])
fill_level = float(sys.argv[3])
fill_rate = float(sys.argv[4])
battery_level = float(sys.argv[5])
weight_kg = float(sys.argv[6])
rssi = float(sys.argv[7])

model = joblib.load(model_path)

data = pd.DataFrame([{
    "hour": hour,
    "day": day,
    "fill_level": fill_level,
    "fill_rate": fill_rate,
    "battery_level": battery_level,
    "weight_kg": weight_kg,
    "rssi": rssi
}])

prediction = model.predict(data)[0]

# نخلي prediction منطقية
prediction = max(fill_level, prediction)
prediction = min(prediction, 100.0)

# priority score بسيطة مبنية على prediction
priority_score = round(prediction / 100.0, 2)

# alert status
if prediction >= 90:
    alert_status = "URGENT"
elif prediction >= 80:
    alert_status = "COLLECT_SOON"
else:
    alert_status = "NORMAL"

# should collect
should_collect = "true" if prediction >= 80 else "false"

print(f"{prediction:.2f},{alert_status},{priority_score:.2f},{should_collect}")