ALTER TABLE bin_predictions
ADD COLUMN IF NOT EXISTS predicted_hours NUMERIC(10,2);

UPDATE bin_predictions bp
SET predicted_hours = latest_btp.predicted_hours
FROM (
    SELECT DISTINCT ON (bin_id)
        bin_id,
        predicted_hours,
        created_at,
        id
    FROM bin_time_predictions
    WHERE predicted_hours IS NOT NULL
    ORDER BY bin_id, created_at DESC, id DESC
) latest_btp
WHERE bp.bin_id = latest_btp.bin_id
  AND bp.predicted_hours IS NULL;