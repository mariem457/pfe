CREATE TABLE routing_execution_logs (
    id BIGSERIAL PRIMARY KEY,
    strategy VARCHAR(30),
    reason TEXT,
    should_optimize BOOLEAN NOT NULL,
    refuel_only BOOLEAN NOT NULL,
    trucks_count INTEGER,
    bins_sent_count INTEGER,
    mandatory_bins_count INTEGER,
    missions_created_count INTEGER,
    dropped_bins_count INTEGER,
    matrix_source VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_routing_execution_logs_created_at
    ON routing_execution_logs(created_at DESC);