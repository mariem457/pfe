ALTER TABLE postponed_bins
ADD COLUMN resolved BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE postponed_bins
ADD COLUMN resolved_at TIMESTAMPTZ NULL;

CREATE INDEX idx_postponed_bins_bin_id_resolved
ON postponed_bins(bin_id, resolved);