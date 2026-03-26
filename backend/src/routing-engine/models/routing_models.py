from pydantic import BaseModel
from typing import List, Optional


class RoutingDepotDto(BaseModel):
    lat: float
    lng: float


class RoutingBinDto(BaseModel):
    id: int
    lat: float
    lng: float
    fillLevel: float
    predictedPriority: float
    estimatedLoadKg: float


class RoutingTruckDto(BaseModel):
    id: int
    lat: Optional[float] = None
    lng: Optional[float] = None
    remainingCapacityKg: float
    fuelLevelLiters: float
    fuelConsumptionPerKm: Optional[float] = None
    status: Optional[str] = None


class RoutingIncidentDto(BaseModel):
    id: int
    truckId: int
    type: str
    severity: str
    description: str


class RoutingRequestDto(BaseModel):
    depot: RoutingDepotDto
    trafficMode: str = "NORMAL"
    bins: List[RoutingBinDto]
    trucks: List[RoutingTruckDto]
    activeIncidents: List[RoutingIncidentDto] = []


class RoutingStopDto(BaseModel):
    binId: int
    orderIndex: int


class RouteCoordinateDto(BaseModel):
    lat: float
    lng: float


class RoutingMissionDto(BaseModel):
    truckId: int
    totalDistanceKm: float
    totalDurationMinutes: float
    stops: List[RoutingStopDto]
    routeCoordinates: List[RouteCoordinateDto] = []


class RoutingResponseDto(BaseModel):
    missions: List[RoutingMissionDto]
    matrixSource: str