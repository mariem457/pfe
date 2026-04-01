CREATE TABLE IF NOT EXISTS system_settings (
    id BIGSERIAL PRIMARY KEY,
    maintenance_mode BOOLEAN NOT NULL DEFAULT FALSE,
    automatic_backup BOOLEAN NOT NULL DEFAULT TRUE,
    realtime_monitoring BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO system_settings (maintenance_mode, automatic_backup, realtime_monitoring)
SELECT FALSE, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM system_settings);