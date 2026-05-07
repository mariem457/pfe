CREATE TABLE bin_sensor_data (
    id BIGSERIAL PRIMARY KEY,
    bin_id VARCHAR(255),
    fill_level DOUBLE PRECISION,
    gas_value INT,
    fire_detected BOOLEAN,
    status VARCHAR(255),
    created_at TIMESTAMP
);