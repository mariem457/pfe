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
    ('BIN-001', 'Centre Ville', 35.5048, 11.0622, 'Bin Centre Ville 1'),
    ('BIN-002', 'Centre Ville', 35.5056, 11.0608, 'Bin Centre Ville 2'),
    ('BIN-003', 'Medina', 35.5034, 11.0648, 'Bin Medina 1'),
    ('BIN-004', 'Medina', 35.5026, 11.0657, 'Bin Medina 2'),
    ('BIN-005', 'Corniche', 35.5092, 11.0718, 'Bin Corniche 1'),
    ('BIN-006', 'Corniche', 35.5081, 11.0703, 'Bin Corniche 2'),
    ('BIN-007', 'Zone Touristique', 35.5121, 11.0756, 'Bin Touristique 1'),
    ('BIN-008', 'Zone Touristique', 35.5110, 11.0742, 'Bin Touristique 2'),
    ('BIN-009', 'Hiboun', 35.4988, 11.0602, 'Bin Hiboun 1'),
    ('BIN-010', 'Hiboun', 35.4996, 11.0589, 'Bin Hiboun 2'),
    ('BIN-011', 'Zahra', 35.5069, 11.0564, 'Bin Zahra 1'),
    ('BIN-012', 'Zahra', 35.5078, 11.0555, 'Bin Zahra 2'),
    ('BIN-013', 'Sidi Messaoud', 35.4978, 11.0662, 'Bin Sidi Messaoud 1'),
    ('BIN-014', 'Borj Erras', 35.5084, 11.0678, 'Bin Borj Erras 1'),
    ('BIN-015', 'Rejiche Road', 35.4992, 11.0724, 'Bin Rejiche Road 1')
) AS v(bin_code, zone_name, lat, lng, notes)
JOIN zones z ON z.name = v.zone_name
ON CONFLICT (bin_code) DO NOTHING;