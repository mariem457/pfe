from config.settings import MATRIX_PROVIDER
from services.osrm_service import get_osrm_matrix
from services.tomtom_service import get_tomtom_matrix


def get_matrix_from_provider(locations):
    provider = (MATRIX_PROVIDER or "OSRM").upper()
    print(f"Configured MATRIX_PROVIDER = {provider}", flush=True)

    if provider == "TOMTOM":
        print("Using TOMTOM matrix provider...", flush=True)
        return get_tomtom_matrix(locations)

    print("Using OSRM matrix provider...", flush=True)
    return get_osrm_matrix(locations)