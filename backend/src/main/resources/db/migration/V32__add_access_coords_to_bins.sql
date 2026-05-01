ALTER TABLE bins
ADD COLUMN access_lat DOUBLE PRECISION NULL,
ADD COLUMN access_lng DOUBLE PRECISION NULL;

UPDATE bins
SET access_lat = lat,
    access_lng = lng
WHERE access_lat IS NULL
   OR access_lng IS NULL;