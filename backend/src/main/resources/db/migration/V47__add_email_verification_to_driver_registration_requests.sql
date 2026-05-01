ALTER TABLE driver_registration_requests
ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE driver_registration_requests
ADD COLUMN email_verification_code VARCHAR(10);

ALTER TABLE driver_registration_requests
ADD COLUMN email_verification_expiry TIMESTAMP WITH TIME ZONE;