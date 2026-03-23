from fastapi import FastAPI
from models.routing_models import RoutingRequestDto, RoutingResponseDto
from services.routing_service import optimize_routing

app = FastAPI()


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/optimize", response_model=RoutingResponseDto)
def optimize(request: RoutingRequestDto):
    return optimize_routing(request)