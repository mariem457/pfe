ALTER TABLE bin_telemetry
DROP CONSTRAINT IF EXISTS bin_telemetry_source_check;

ALTER TABLE bin_telemetry
ADD CONSTRAINT bin_telemetry_source_check
CHECK (source IN ('MQTT_REAL', 'MQTT_SIM', 'PY_SIM'));
