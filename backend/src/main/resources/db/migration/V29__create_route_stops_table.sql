CREATE TABLE IF NOT EXISTS route_stops (
    id BIGSERIAL PRIMARY KEY,
    route_plan_id BIGINT NOT NULL REFERENCES route_plans(id) ON DELETE CASCADE,
    stop_order INT NOT NULL,
    stop_type VARCHAR(20) NOT NULL CHECK (
        stop_type IN ('DEPOT_START','BIN_PICKUP','FUEL_STATION','DEPOT_RETURN','EMERGENCY_STOP')
    ),
    bin_id BIGINT REFERENCES bins(id) ON DELETE SET NULL,
    fuel_station_id BIGINT REFERENCES fuel_stations(id) ON DELETE SET NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    estimated_arrival TIMESTAMPTZ,
    actual_arrival TIMESTAMPTZ,
    estimated_departure TIMESTAMPTZ,
    actual_departure TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    delay_minutes INT DEFAULT 0,
    notes TEXT,
    UNIQUE(route_plan_id, stop_order)
);