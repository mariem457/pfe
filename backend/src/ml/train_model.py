import pandas as pd
import joblib
from xgboost import XGBRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error, r2_score

# تحميل dataset
df = pd.read_csv("dataset_ml.csv")

features = [
    "hour",
    "day",
    "fill_level",
    "fill_rate",
    "battery_level",
    "weight_kg",
    "rssi"
]

X = df[features]
y = df["fill_level_next"]

# تقسيم البيانات
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)

# إنشاء model
model = XGBRegressor(
    n_estimators=400,
    max_depth=7,
    learning_rate=0.05,
    subsample=0.8,
    colsample_bytree=0.8,
    random_state=42
)

# التدريب
model.fit(X_train, y_train)

# prediction
y_pred = model.predict(X_test)

# evaluation
mae = mean_absolute_error(y_test, y_pred)
r2 = r2_score(y_test, y_pred)

print("MAE:", mae)
print("R2:", r2)

# حفظ model
joblib.dump(model, "smart_bin_model.pkl")

print("Model saved")
comparison = pd.DataFrame({
    "real": y_test,
    "predicted": y_pred
})

print(comparison.head(10))
