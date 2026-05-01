import json
import time
import random
from datetime import datetime, timezone
import paho.mqtt.client as mqtt

BROKER = "localhost"   # كان ما يخدمش جرّب 127.0.0.1
PORT = 1883
BIN_CODE = "BIN-001"
TOPIC = f"bins/{BIN_CODE}/telemetry"

client = mqtt.Client(client_id=f"py-sim-{BIN_CODE}")
client.connect(BROKER, PORT, 60)
client.loop_start()

fill = 20

def get_activity_period():
    hour = datetime.now().hour

    if 0 <= hour < 6:
        return "night"
    elif 6 <= hour < 12:
        return "morning"
    elif 12 <= hour < 18:
        return "afternoon"
    else:
        return "evening"

def get_fill_increment(period):
    if period == "night":
        return random.randint(0, 1)
    elif period == "morning":
        return random.randint(1, 3)
    elif period == "afternoon":
        return random.randint(2, 5)
    else:  # evening
        return random.randint(3, 6)

def is_event():
    # 5% chance of special event
    return random.random() < 0.05

try:
    while True:
        period = get_activity_period()
        increment = get_fill_increment(period)

        # event mode
        if is_event():
            increment += random.randint(4, 10)
            print("EVENT MODE: extra waste detected")

        fill = min(100, fill + increment)

        # status
        if fill >= 95:
            status = "FULL"
        elif fill >= 80:
            status = "WARNING"
        else:
            status = "OK"

        payload = {
            "binCode": BIN_CODE,
            "fillLevel": fill,
            "batteryLevel": random.randint(60, 100),
            "status": status,
            "source": "PY_SIM",
            "timestamp": datetime.now(timezone.utc).isoformat()
        }

        msg = json.dumps(payload)
        client.publish(TOPIC, msg, qos=1)
        print(f"sent to {TOPIC} | period={period} | fill={fill}% | status={status}")

        # إذا poubelle تعبات، نعملو vidage بعد دقيقة
        if fill >= 100:
            print("BIN FULL -> collection simulated")
            time.sleep(60)
            fill = random.randint(5, 15)
            print(f"Bin emptied, new fill level = {fill}%")

        # send every 60 seconds instead of 2 seconds
        time.sleep(60)

except KeyboardInterrupt:
    print("Stopping...")

finally:
    client.loop_stop()
    client.disconnect()