import os
from dotenv import load_dotenv

load_dotenv()


def get_str(name: str, default: str) -> str:
    return os.getenv(name, default)


def get_int(name: str, default: int) -> int:
    value = os.getenv(name)
    if value is None or value == "":
        return default
    return int(value)


def get_float(name: str, default: float) -> float:
    value = os.getenv(name)
    if value is None or value == "":
        return default
    return float(value)


OSRM_TABLE_URL = get_str("OSRM_TABLE_URL", "http://localhost:5000/table/v1/driving")
OSRM_ROUTE_URL = get_str("OSRM_ROUTE_URL", "http://localhost:5000/route/v1/driving")
OSRM_TIMEOUT = get_int("OSRM_TIMEOUT", 8)

FALLBACK_SPEED_KMH = get_float("FALLBACK_SPEED_KMH", 30.0)
DEFAULT_MAX_ROUTE_MINUTES = get_int("DEFAULT_MAX_ROUTE_MINUTES", 480)

DEFAULT_TRAFFIC_MODE = get_str("DEFAULT_TRAFFIC_MODE", "NORMAL")
HEAVY_TRAFFIC_FACTOR = get_float("HEAVY_TRAFFIC_FACTOR", 1.35)
LIGHT_TRAFFIC_FACTOR = get_float("LIGHT_TRAFFIC_FACTOR", 0.90)

MATRIX_PROVIDER = get_str("MATRIX_PROVIDER", "OSRM")
TOMTOM_API_KEY = get_str("TOMTOM_API_KEY", "")
TOMTOM_TIMEOUT = get_int("TOMTOM_TIMEOUT", 10)
TOMTOM_POLL_INTERVAL_SECONDS = 2
TOMTOM_MAX_WAIT_SECONDS = 30