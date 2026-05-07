-- حذف constraint القديم
ALTER TABLE alerts
DROP CONSTRAINT IF EXISTS alerts_entity_type_check;

-- إعادة إنشاء constraint مع القيمة الجديدة
ALTER TABLE alerts
ADD CONSTRAINT alerts_entity_type_check
CHECK (
    entity_type IN (
        'BIN',
        'TRUCK',
        'MISSION',
        'INCIDENT',
        'MUNICIPAL_EXCEPTION'
    )
);