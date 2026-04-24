from typing import List, Optional

from pydantic import BaseModel, Field


class RoutingDepotDto(BaseModel):
    lat: float
    lng: float


class RoutingBinDto(BaseModel):
    id: int
    lat: float
    lng: float

    zoneId: Optional[int] = None
    clusterId: Optional[int] = None

    fillLevel: float
    predictedPriority: float
    estimatedLoadKg: float
    predictedHoursToFull: Optional[float] = None
    mandatory: Optional[bool] = None

    wasteType: Optional[str] = None

    decisionCategory: Optional[str] = None
    decisionReason: Optional[str] = None
    feedbackScore: Optional[float] = None
    postponementCount: Optional[int] = None
    opportunistic: Optional[bool] = None
    reportable: Optional[bool] = None
    opportunisticScore: Optional[float] = None

    windowStartMinutes: Optional[int] = None
    windowEndMinutes: Optional[int] = None


class RoutingTruckDto(BaseModel):
    id: int
    lat: Optional[float] = None
    lng: Optional[float] = None
    remainingCapacityKg: float
    fuelLevelLiters: Optional[float] = None
    fuelConsumptionPerKm: Optional[float] = None
    status: Optional[str] = None
    supportedWasteTypes: List[str] = Field(default_factory=list)

    zoneId: Optional[int] = None


class RoutingIncidentDto(BaseModel):
    id: int
    truckId: int
    type: str
    severity: str
    description: str


class RoutingRequestDto(BaseModel):
    depot: RoutingDepotDto
    trafficMode: str = "NORMAL"
    currentRun: Optional[str] = None
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
    droppedBinIds: List[int] = Field(default_factory=list)