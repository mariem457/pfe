-- =========================================
-- Reposition existing bins (1 -> 22)
-- Better spread across Mahdia
-- =========================================

UPDATE bins SET lat = 35.5052, lng = 11.0614, notes = 'Centre Ville 1' WHERE bin_code = 'BIN-001';
UPDATE bins SET lat = 35.5044, lng = 11.0598, notes = 'Centre Ville 2' WHERE bin_code = 'BIN-002';
UPDATE bins SET lat = 35.5036, lng = 11.0642, notes = 'Medina 1' WHERE bin_code = 'BIN-003';
UPDATE bins SET lat = 35.5024, lng = 11.0660, notes = 'Medina 2' WHERE bin_code = 'BIN-004';
UPDATE bins SET lat = 35.5074, lng = 11.0678, notes = 'Corniche 1' WHERE bin_code = 'BIN-005';
UPDATE bins SET lat = 35.5086, lng = 11.0701, notes = 'Corniche 2' WHERE bin_code = 'BIN-006';
UPDATE bins SET lat = 35.5107, lng = 11.0735, notes = 'Zone Touristique 1' WHERE bin_code = 'BIN-007';
UPDATE bins SET lat = 35.5120, lng = 11.0754, notes = 'Zone Touristique 2' WHERE bin_code = 'BIN-008';
UPDATE bins SET lat = 35.4989, lng = 11.0593, notes = 'Hiboun 1' WHERE bin_code = 'BIN-009';
UPDATE bins SET lat = 35.5003, lng = 11.0579, notes = 'Hiboun 2' WHERE bin_code = 'BIN-010';
UPDATE bins SET lat = 35.5069, lng = 11.0559, notes = 'Zahra 1' WHERE bin_code = 'BIN-011';
UPDATE bins SET lat = 35.5081, lng = 11.0548, notes = 'Zahra 2' WHERE bin_code = 'BIN-012';
UPDATE bins SET lat = 35.4976, lng = 11.0658, notes = 'Sidi Messaoud 1' WHERE bin_code = 'BIN-013';
UPDATE bins SET lat = 35.5048, lng = 11.0681, notes = 'Borj Erras 1' WHERE bin_code = 'BIN-014';
UPDATE bins SET lat = 35.5008, lng = 11.0717, notes = 'Rejiche Road 1' WHERE bin_code = 'BIN-015';
UPDATE bins SET lat = 35.5060, lng = 11.0626, notes = 'Centre Ville 3' WHERE bin_code = 'BIN-016';
UPDATE bins SET lat = 35.5018, lng = 11.0672, notes = 'Medina 3' WHERE bin_code = 'BIN-017';
UPDATE bins SET lat = 35.5091, lng = 11.0688, notes = 'Corniche 3' WHERE bin_code = 'BIN-018';
UPDATE bins SET lat = 35.5113, lng = 11.0719, notes = 'Zone Touristique 3' WHERE bin_code = 'BIN-019';
UPDATE bins SET lat = 35.4981, lng = 11.0619, notes = 'Hiboun 3' WHERE bin_code = 'BIN-020';
UPDATE bins SET lat = 35.5058, lng = 11.0572, notes = 'Zahra 3' WHERE bin_code = 'BIN-021';
UPDATE bins SET lat = 35.4968, lng = 11.0671, notes = 'Sidi Messaoud 2' WHERE bin_code = 'BIN-022';

-- =========================================
-- Add 8 new bins to fill empty areas
-- Total => 30 bins
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
    ('BIN-023', 'Centre Ville',     35.5039, 11.0609, 'Centre Ville 4'),
    ('BIN-024', 'Medina',           35.5014, 11.0649, 'Medina 4'),
    ('BIN-025', 'Corniche',         35.5079, 11.0659, 'Corniche 4'),
    ('BIN-026', 'Zone Touristique', 35.5132, 11.0740, 'Zone Touristique 4'),
    ('BIN-027', 'Hiboun',           35.4972, 11.0584, 'Hiboun 4'),
    ('BIN-028', 'Mahdia Sud',       35.4949, 11.0612, 'Mahdia Sud 1'),
    ('BIN-029', 'Mahdia Nord',      35.5146, 11.0638, 'Mahdia Nord 1'),
    ('BIN-030', 'Borj Erras',       35.5031, 11.0696, 'Borj Erras 2')
) AS v(bin_code, zone_name, lat, lng, notes)
JOIN zones z ON z.name = v.zone_name
ON CONFLICT (bin_code) DO NOTHING;