CREATE TABLE IF NOT EXISTS public_reports (
    id BIGSERIAL PRIMARY KEY,
    report_code VARCHAR(40) UNIQUE NOT NULL,
    report_type VARCHAR(30) NOT NULL CHECK (
        report_type IN ('BIN_FULL','OVERFLOW','ILLEGAL_DUMP','BIN_DAMAGED','MISSED_COLLECTION','OTHER')
    ),
    photo_url TEXT,
    description TEXT,
    address TEXT,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE' CHECK (
        status IN ('EN_ATTENTE','AFFECTE','RESOLU','REJETE')
    ),
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' CHECK (
        priority IN ('LOW','MEDIUM','HIGH')
    ),
    assigned_driver_id BIGINT REFERENCES drivers(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    resolved_note TEXT
);

CREATE INDEX IF NOT EXISTS idx_public_reports_status_created
ON public_reports (status, created_at DESC);