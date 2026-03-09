-- Delete telemetry of bins outside Mahdia
DELETE FROM bin_telemetry
WHERE bin_id IN (
    SELECT id FROM bins
    WHERE NOT (
        lat BETWEEN 35.48 AND 35.56
        AND lng BETWEEN 11.02 AND 11.12
    )
);

-- Delete bins outside Mahdia
DELETE FROM bins
WHERE NOT (
    lat BETWEEN 35.48 AND 35.56
    AND lng BETWEEN 11.02 AND 11.12
);