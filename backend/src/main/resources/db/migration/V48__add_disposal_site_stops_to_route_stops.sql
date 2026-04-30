ALTER TABLE route_stops
ADD COLUMN IF NOT EXISTS disposal_site_id BIGINT;

ALTER TABLE route_stops
ADD COLUMN IF NOT EXISTS disposal_site_name VARCHAR(255);

ALTER TABLE route_stops
ALTER COLUMN stop_type TYPE VARCHAR(30);