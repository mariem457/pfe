ALTER TABLE alerts
    ALTER COLUMN bin_id DROP NOT NULL;

ALTER TABLE alerts
    DROP CONSTRAINT IF EXISTS alerts_alert_type_check;

ALTER TABLE alerts
    DROP CONSTRAINT IF EXISTS alerts_severity_check;

ALTER TABLE alerts
    ADD COLUMN IF NOT EXISTS entity_type VARCHAR(30) NOT NULL DEFAULT 'BIN',
    ADD COLUMN IF NOT EXISTS entity_id BIGINT,
    ADD COLUMN IF NOT EXISTS truck_id BIGINT REFERENCES trucks(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS mission_id BIGINT REFERENCES missions(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS incident_id BIGINT REFERENCES truck_incidents(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS recommendation TEXT,
    ADD COLUMN IF NOT EXISTS action_type VARCHAR(40);

UPDATE alerts
SET entity_type = 'BIN',
    entity_id = bin_id
WHERE entity_id IS NULL AND bin_id IS NOT NULL;

ALTER TABLE alerts
    ADD CONSTRAINT alerts_entity_type_check
    CHECK (entity_type IN ('BIN','TRUCK','MISSION','INCIDENT','SYSTEM'));

ALTER TABLE alerts
    ADD CONSTRAINT alerts_severity_check
    CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL'));

CREATE INDEX IF NOT EXISTS idx_alerts_entity
    ON alerts(entity_type, entity_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_alerts_truck_created
    ON alerts(truck_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_alerts_mission_created
    ON alerts(mission_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_alerts_incident_created
    ON alerts(incident_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_alerts_open_type
    ON alerts(resolved, alert_type, severity);