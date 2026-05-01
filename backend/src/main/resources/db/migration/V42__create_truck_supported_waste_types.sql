CREATE TABLE IF NOT EXISTS truck_supported_waste_types (
    truck_id BIGINT NOT NULL,
    waste_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_truck_supported_waste_types
        PRIMARY KEY (truck_id, waste_type),

    CONSTRAINT fk_t_s_w_t_truck
        FOREIGN KEY (truck_id)
        REFERENCES trucks(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_t_s_w_t_truck_id
    ON truck_supported_waste_types(truck_id);

CREATE INDEX IF NOT EXISTS idx_t_s_w_t_waste_type
    ON truck_supported_waste_types(waste_type);