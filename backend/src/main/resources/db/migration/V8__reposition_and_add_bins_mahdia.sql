-- =========================================
-- Reposition existing 15 bins inside Mahdia
-- =========================================

UPDATE bins SET lat = 35.5048, lng = 11.0622, notes = 'Centre Ville 1' WHERE bin_code = 'BIN-001';
UPDATE bins SET lat = 35.5056, lng = 11.0608, notes = 'Centre Ville 2' WHERE bin_code = 'BIN-002';
UPDATE bins SET lat = 35.5034, lng = 11.0648, notes = 'Medina 1'       WHERE bin_code = 'BIN-003';
UPDATE bins SET lat = 35.5026, lng = 11.0657, notes = 'Medina 2'       WHERE bin_code = 'BIN-004';
UPDATE bins SET lat = 35.5078, lng = 11.0684, notes = 'Corniche 1'     WHERE bin_code = 'BIN-005';
UPDATE bins SET lat = 35.5069, lng = 11.0702, notes = 'Corniche 2'     WHERE bin_code = 'BIN-006';
UPDATE bins SET lat = 35.5094, lng = 11.0731, notes = 'Zone Touristique 1' WHERE bin_code = 'BIN-007';
UPDATE bins SET lat = 35.5102, lng = 11.0744, notes = 'Zone Touristique 2' WHERE bin_code = 'BIN-008';
UPDATE bins SET lat = 35.4991, lng = 11.0601, notes = 'Hiboun 1'       WHERE bin_code = 'BIN-009';
UPDATE bins SET lat = 35.5000, lng = 11.0589, notes = 'Hiboun 2'       WHERE bin_code = 'BIN-010';
UPDATE bins SET lat = 35.5069, lng = 11.0564, notes = 'Zahra 1'        WHERE bin_code = 'BIN-011';
UPDATE bins SET lat = 35.5078, lng = 11.0555, notes = 'Zahra 2'        WHERE bin_code = 'BIN-012';
UPDATE bins SET lat = 35.4978, lng = 11.0662, notes = 'Sidi Messaoud 1' WHERE bin_code = 'BIN-013';
UPDATE bins SET lat = 35.5046, lng = 11.0673, notes = 'Borj Erras 1'   WHERE bin_code = 'BIN-014';
UPDATE bins SET lat = 35.5008, lng = 11.0712, notes = 'Rejiche Road 1' WHERE bin_code = 'BIN-015';

-- =========================================
-- Add 7 new bins inside Mahdia
-- =========================================

INSERT INTO bins (
    bin_code,
    type,
    zone_id,
    lat,
    lng,
    installation_date,
    is_active,
    notes,
    created_at,
    updated_at
)
SELECT
    v.bin_code,
    'SIM',
    z.id,
    v.lat,
    v.lng,
    CURRENT_DATE,
    TRUE,
    v.notes,
    NOW(),
    NOW()
FROM (
    VALUES
    ('BIN-016', 'Centre Ville',     35.5041, 11.0610, 'Centre Ville 3'),
    ('BIN-017', 'Medina',           35.5039, 11.0663, 'Medina 3'),
    ('BIN-018', 'Corniche',         35.5088, 11.0691, 'Corniche 3'),
    ('BIN-019', 'Zone Touristique', 35.5113, 11.0728, 'Zone Touristique 3'),
    ('BIN-020', 'Hiboun',           35.4983, 11.0617, 'Hiboun 3'),
    ('BIN-021', 'Zahra',            35.5060, 11.0575, 'Zahra 3'),
    ('BIN-022', 'Sidi Messaoud',    35.4969, 11.0670, 'Sidi Messaoud 2')
) AS v(bin_code, zone_name, lat, lng, notes)
JOIN zones z ON z.name = v.zone_name
ON CONFLICT (bin_code) DO NOTHING;