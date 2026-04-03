CREATE TABLE IF NOT EXISTS bin_time_predictions (
    id BIGSERIAL PRIMARY KEY,
    bin_id BIGINT NOT NULL,
    telemetry_id BIGINT NOT NULL,
    predicted_hours NUMERIC(10,2),
    alert_status VARCHAR(255),
    priority_score NUMERIC(10,2),
    should_collect BOOLEAN,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_bin_time_predictions_bin
        FOREIGN KEY (bin_id) REFERENCES bins(id) ON DELETE CASCADE,

    CONSTRAINT fk_bin_time_predictions_telemetry
        FOREIGN KEY (telemetry_id) REFERENCES bin_telemetry(id) ON DELETE CASCADE
);