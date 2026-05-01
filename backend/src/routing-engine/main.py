from fastapi import FastAPI
from models.routing_models import RoutingRequestDto, RoutingResponseDto
from services.routing_service import optimize_routing
from config.settings import (
    MATRIX_PROVIDER,
    OSRM_TABLE_URL,
    OSRM_ROUTE_URL,
    TOMTOM_API_KEY
)

app = FastAPI()


@app.get("/health")
def health():
    return {
        "status": "ok",
        "matrixProvider": MATRIX_PROVIDER,
        "osrmTableUrl": OSRM_TABLE_URL,
        "osrmRouteUrl": OSRM_ROUTE_URL,
        "tomtomKeyConfigured": bool(TOMTOM_API_KEY and TOMTOM_API_KEY.strip())
    }


@app.post("/optimize", response_model=RoutingResponseDto)
def optimize(request: RoutingRequestDto):
    print(
        f"/optimize called | bins={len(request.bins)} | trucks={len(request.trucks)} | activeIncidents={len(request.activeIncidents)}",
        flush=True
    )
    return optimize_routing(request)