-- =========================
-- SMART WASTE DB - Neon Postgres
-- =========================

-- (Optional) clean start
-- DROP SCHEMA public CASCADE;
-- CREATE SCHEMA public;

-- -------------------------
-- 1) USERS
-- -------------------------
CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  email VARCHAR(120) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','MUNICIPALITY','DRIVER')),
  is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  last_login_at TIMESTAMPTZ NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -------------------------
-- 2) DRIVERS
-- -------------------------
CREATE TABLE IF NOT EXISTS drivers (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  full_name VARCHAR(120) NOT NULL,
  phone VARCHAR(30),
  vehicle_code VARCHAR(50),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -------------------------
-- 3) ZONES
-- -------------------------
CREATE TABLE IF NOT EXISTS zones (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(80) UNIQUE NOT NULL,
  description TEXT,
  center_lat DOUBLE PRECISION,
  center_lng DOUBLE PRECISION,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -------------------------
-- 4) BINS
-- -------------------------
CREATE TABLE IF NOT EXISTS bins (
  id BIGSERIAL PRIMARY KEY,
  bin_code VARCHAR(30) UNIQUE NOT NULL,
  type VARCHAR(10) NOT NULL CHECK (type IN ('REAL','SIM')),
  zone_id BIGINT REFERENCES zones(id) ON DELETE SET NULL,
  lat DOUBLE PRECISION NOT NULL,
  lng DOUBLE PRECISION NOT NULL,
  installation_date DATE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -------------------------
-- 5) BIN TELEMETRY
-- -------------------------
CREATE TABLE IF NOT EXISTS bin_telemetry (
  id BIGSERIAL PRIMARY KEY,
  bin_id BIGINT NOT NULL REFERENCES bins(id) ON DELETE CASCADE,
  timestamp TIMESTAMPTZ NOT NULL,
  fill_level SMALLINT NOT NULL CHECK (fill_level BETWEEN 0 AND 100),
  weight_kg NUMERIC(8,2),
  battery_level SMALLINT CHECK (battery_level BETWEEN 0 AND 100),
  status VARCHAR(20) NOT NULL DEFAULT 'OK'
    CHECK (status IN ('OK','WARNING','FULL','OVERFLOW','ERROR')),
  rssi SMALLINT,
  source VARCHAR(15) NOT NULL CHECK (source IN ('MQTT_REAL','MQTT_SIM')),
  raw_payload JSONB
);

CREATE INDEX IF NOT EXISTS idx_telemetry_bin_time
ON bin_telemetry (bin_id, timestamp DESC);

-- -------------------------
-- 6) ALERTS
-- -------------------------
CREATE TABLE IF NOT EXISTS alerts (
  id BIGSERIAL PRIMARY KEY,
  bin_id BIGINT NOT NULL REFERENCES bins(id) ON DELETE CASCADE,
  telemetry_id BIGINT REFERENCES bin_telemetry(id) ON DELETE SET NULL,
  alert_type VARCHAR(20) NOT NULL CHECK (alert_type IN ('THRESHOLD','ANOMALY','MAINTENANCE','SYSTEM')),
  severity VARCHAR(10) NOT NULL CHECK (severity IN ('LOW','MEDIUM','HIGH')),
  title VARCHAR(120) NOT NULL,
  message TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  resolved BOOLEAN NOT NULL DEFAULT FALSE,
  resolved_at TIMESTAMPTZ,
  resolved_by BIGINT REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_alerts_bin_created
ON alerts (bin_id, created_at DESC);

-- -------------------------
-- 7) ANOMALIES (IA)
-- -------------------------
CREATE TABLE IF NOT EXISTS anomalies (
  id BIGSERIAL PRIMARY KEY,
  bin_id BIGINT NOT NULL REFERENCES bins(id) ON DELETE CASCADE,
  start_time TIMESTAMPTZ NOT NULL,
  end_time TIMESTAMPTZ,
  anomaly_type VARCHAR(20) NOT NULL CHECK (anomaly_type IN ('DRIFT','STUCK','OUTLIER','PACKET_LOSS')),
  score NUMERIC(5,2) NOT NULL,
  details JSONB,
  detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  model_name VARCHAR(50),
  is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_anomalies_bin_detected
ON anomalies (bin_id, detected_at DESC);

-- -------------------------
-- 8) MISSIONS
-- -------------------------
CREATE TABLE IF NOT EXISTS missions (
  id BIGSERIAL PRIMARY KEY,
  mission_code VARCHAR(40) UNIQUE NOT NULL,
  driver_id BIGINT NOT NULL REFERENCES drivers(id) ON DELETE RESTRICT,
  zone_id BIGINT REFERENCES zones(id) ON DELETE SET NULL,
  status VARCHAR(20) NOT NULL CHECK (status IN ('CREATED','IN_PROGRESS','COMPLETED','CANCELLED')),
  priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('LOW','NORMAL','HIGH')),
  planned_date DATE NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
  notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_missions_driver_date
ON missions (driver_id, planned_date DESC);

-- -------------------------
-- 9) MISSION_BINS
-- -------------------------
CREATE TABLE IF NOT EXISTS mission_bins (
  id BIGSERIAL PRIMARY KEY,
  mission_id BIGINT NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
  bin_id BIGINT NOT NULL REFERENCES bins(id) ON DELETE RESTRICT,
  visit_order INT NOT NULL,
  target_fill_threshold SMALLINT,
  assigned_reason VARCHAR(20) CHECK (assigned_reason IN ('THRESHOLD','PREDICTION','MANUAL')),
  collected BOOLEAN NOT NULL DEFAULT FALSE,
  collected_at TIMESTAMPTZ,
  collected_by BIGINT REFERENCES drivers(id) ON DELETE SET NULL,
  driver_note TEXT,
  issue_type VARCHAR(30) CHECK (issue_type IN ('BLOCKED','DAMAGED','SENSOR_ERROR','OTHER')),
  photo_url TEXT,
  UNIQUE (mission_id, bin_id)
);

CREATE INDEX IF NOT EXISTS idx_mission_bins_mission_order
ON mission_bins (mission_id, visit_order);

-- -------------------------
-- 10) KPI DAILY (OPTIONAL)
-- -------------------------
CREATE TABLE IF NOT EXISTS kpi_daily (
  id BIGSERIAL PRIMARY KEY,
  date DATE UNIQUE NOT NULL,
  total_bins INT NOT NULL DEFAULT 0,
  bins_overflow_count INT NOT NULL DEFAULT 0,
  alerts_count INT NOT NULL DEFAULT 0,
  missions_count INT NOT NULL DEFAULT 0,
  collected_bins_count INT NOT NULL DEFAULT 0,
  avg_fill_level NUMERIC(5,2),
  estimated_distance_km NUMERIC(10,2),
  estimated_cost NUMERIC(10,2),
  estimated_co2_kg NUMERIC(10,2),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);