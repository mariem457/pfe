CREATE TABLE IF NOT EXISTS zones (
    id BIGSERIAL PRIMARY KEY,
    shape_name VARCHAR(255) NOT NULL,
    shape_id VARCHAR(255) NOT NULL UNIQUE,
    shape_type VARCHAR(50),
    shape_group VARCHAR(50),
    geometry_json JSONB NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_zones_shape_name
    ON zones(shape_name);

CREATE INDEX IF NOT EXISTS idx_zones_shape_group
    ON zones(shape_group);