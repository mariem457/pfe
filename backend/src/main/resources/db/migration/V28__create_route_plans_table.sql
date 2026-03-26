CREATE TABLE IF NOT EXISTS route_plans (
    id BIGSERIAL PRIMARY KEY,
    mission_id BIGINT REFERENCES missions(id) ON DELETE CASCADE,
    truck_id BIGINT NOT NULL REFERENCES trucks(id) ON DELETE CASCADE,
    depot_id BIGINT REFERENCES depots(id) ON DELETE SET NULL,
    plan_type VARCHAR(20) NOT NULL DEFAULT 'INITIAL' CHECK (
        plan_type IN ('INITIAL','REPLANNED','EMERGENCY')
    ),
    optimization_algorithm VARCHAR(100),
    optimization_version VARCHAR(50),
    total_distance_km NUMERIC(10,2),
    estimated_duration_min INT,
    estimated_fuel_liters NUMERIC(10,2),
    estimated_cost NUMERIC(10,2),
    traffic_mode VARCHAR(20),
    plan_status VARCHAR(20) NOT NULL DEFAULT 'PLANNED' CHECK (
        plan_status IN ('PLANNED','ACTIVE','REPLACED','CANCELLED','COMPLETED')
    ),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);