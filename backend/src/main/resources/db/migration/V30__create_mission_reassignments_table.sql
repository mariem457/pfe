CREATE TABLE IF NOT EXISTS mission_reassignments (
    id BIGSERIAL PRIMARY KEY,
    original_mission_id BIGINT NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    source_truck_id BIGINT REFERENCES trucks(id) ON DELETE SET NULL,
    target_truck_id BIGINT REFERENCES trucks(id) ON DELETE SET NULL,
    bin_id BIGINT REFERENCES bins(id) ON DELETE SET NULL,
    reason VARCHAR(30) NOT NULL CHECK (
        reason IN ('BREAKDOWN','TRAFFIC','FUEL_LOW','DELAY','MANUAL','OTHER')
    ),
    reassigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    algorithm_version VARCHAR(50),
    notes TEXT
);