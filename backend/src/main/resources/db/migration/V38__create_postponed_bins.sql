CREATE TABLE postponed_bins (
    id BIGSERIAL PRIMARY KEY,

    bin_id BIGINT NOT NULL,
    routing_mission_id BIGINT NULL,

    reason VARCHAR(100) NOT NULL,

    predicted_priority DOUBLE PRECISION NULL,
    predicted_hours_to_full DOUBLE PRECISION NULL,
    fill_level DOUBLE PRECISION NULL,
    estimated_load_kg DOUBLE PRECISION NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_postponed_bins_bin
        FOREIGN KEY (bin_id)
        REFERENCES bins(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_postponed_bins_mission
        FOREIGN KEY (routing_mission_id)
        REFERENCES missions(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_postponed_bins_bin_id
    ON postponed_bins(bin_id);

CREATE INDEX idx_postponed_bins_created_at
    ON postponed_bins(created_at);

CREATE INDEX idx_postponed_bins_reason
    ON postponed_bins(reason);