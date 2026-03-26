CREATE TABLE IF NOT EXISTS trucks (
    id BIGSERIAL PRIMARY KEY,
    truck_code VARCHAR(50) UNIQUE NOT NULL,
    plate_number VARCHAR(30) UNIQUE,
    model VARCHAR(80),
    brand VARCHAR(80),
    fuel_type VARCHAR(20) NOT NULL CHECK (fuel_type IN ('DIESEL','ESSENCE','ELECTRIC','HYBRID')),
    tank_capacity_liters NUMERIC(10,2),
    fuel_level_liters NUMERIC(10,2),
    fuel_consumption_per_km NUMERIC(10,4),
    max_load_kg NUMERIC(10,2),
    max_bin_capacity INT,
    current_load_kg NUMERIC(10,2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL CHECK (
        status IN ('AVAILABLE','ON_MISSION','BREAKDOWN','MAINTENANCE','REFUELING','UNAVAILABLE','OUT_OF_SERVICE')
    ),
    assigned_driver_id BIGINT REFERENCES drivers(id) ON DELETE SET NULL,
    last_known_lat DOUBLE PRECISION,
    last_known_lng DOUBLE PRECISION,
    last_status_update TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);