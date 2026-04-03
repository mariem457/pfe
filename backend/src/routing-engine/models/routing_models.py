from pydantic import BaseModel, Field
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
    activeIncidents: List[RoutingIncidentDto] = Field(default_factory=list)


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
    routeCoordinates: List[RouteCoordinateDto] = Field(default_factory=list)


class ExcludedTruckDto(BaseModel):
    truckId: int
    reason: str


class WarningTruckDto(BaseModel):
    truckId: int
    reason: str


class RecommendedFuelStationDto(BaseModel):
    truckId: int
    stationId: int
    stationName: str
    lat: float
    lng: float


class RoutingResponseDto(BaseModel):
    missions: List[RoutingMissionDto]
    matrixSource: str
    excludedTrucks: List[ExcludedTruckDto] = Field(default_factory=list)
    warningTrucks: List[WarningTruckDto] = Field(default_factory=list)
    recommendedFuelStations: List[RecommendedFuelStationDto] = Field(default_factory=list)