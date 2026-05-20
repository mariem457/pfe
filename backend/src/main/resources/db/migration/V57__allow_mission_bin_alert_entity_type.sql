ALTER TABLE alerts
DROP CONSTRAINT IF EXISTS alerts_entity_type_check;

ALTER TABLE alerts
ADD CONSTRAINT alerts_entity_type_check
CHECK (
    entity_type IN (
        'BIN',
        'TRUCK',
        'MISSION',
        'MISSION_BIN',
        'INCIDENT',
        'MUNICIPAL_EXCEPTION'
    )
);
