CREATE TABLE IF NOT EXISTS truck_locations (
  id BIGSERIAL PRIMARY KEY,
  driver_id BIGINT NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
  timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  lat DOUBLE PRECISION NOT NULL,
  lng DOUBLE PRECISION NOT NULL,
  speed_kmh NUMERIC(6,2),
  heading_deg NUMERIC(6,2)
);

CREATE INDEX IF NOT EXISTS idx_truck_loc_driver_time
ON truck_locations(driver_id, timestamp DESC);