CREATE TABLE IF NOT EXISTS truck_incidents (
    id BIGSERIAL PRIMARY KEY,
    truck_id BIGINT NOT NULL REFERENCES trucks(id) ON DELETE CASCADE,
    mission_id BIGINT REFERENCES missions(id) ON DELETE SET NULL,
    incident_type VARCHAR(30) NOT NULL CHECK (
        incident_type IN ('BREAKDOWN','FUEL_LOW','GPS_LOST','TRAFFIC_BLOCK','DELAY','OVERLOAD','DRIVER_UNAVAILABLE','OTHER')
    ),
    severity VARCHAR(10) NOT NULL CHECK (
        severity IN ('LOW','MEDIUM','HIGH','CRITICAL')
    ),
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (
        status IN ('OPEN','IN_PROGRESS','RESOLVED','CANCELLED')
    ),
    reported_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    auto_detected BOOLEAN NOT NULL DEFAULT FALSE,
    lat DOUBLE PRECISION,
    lng DOUBLE PRECISION,
    reported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);