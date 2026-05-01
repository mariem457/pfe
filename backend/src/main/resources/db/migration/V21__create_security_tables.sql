CREATE TABLE IF NOT EXISTS security_settings (
    id BIGSERIAL PRIMARY KEY,
    two_factor_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    login_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    api_rate_limiting BOOLEAN NOT NULL DEFAULT TRUE,
    suspicious_activity_detection BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO security_settings (
    two_factor_enabled,
    login_notifications,
    api_rate_limiting,
    suspicious_activity_detection
)
SELECT TRUE, TRUE, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM security_settings);

CREATE TABLE IF NOT EXISTS security_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(40) NOT NULL,
    title VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    username VARCHAR(120),
    device VARCHAR(80),
    ip_address VARCHAR(80),
    location VARCHAR(120),
    event_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS api_keys (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    key_value VARCHAR(255) NOT NULL UNIQUE,
    is_test BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO api_keys (name, key_value, is_test, is_active)
SELECT 'Clé API principale', 'sk-principale-7F4A-92KD-118X', FALSE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM api_keys WHERE name = 'Clé API principale');

INSERT INTO api_keys (name, key_value, is_test, is_active)
SELECT 'Clé API de test', 'sk-test-2D8Q-54LM-773P', TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM api_keys WHERE name = 'Clé API de test');