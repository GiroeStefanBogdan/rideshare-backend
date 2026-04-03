-- Add coordinates
ALTER TABLE admin_units_stage
ADD COLUMN latitude double precision,
ADD COLUMN longitude double precision;

UPDATE admin_units_stage
SET
    latitude = ST_Y(ST_PointOnSurface(geom)),
    longitude = ST_X(ST_PointOnSurface(geom));

-- Normalize street names
ALTER TABLE streets_stage ADD COLUMN name_norm text;

UPDATE streets_stage
SET name_norm = lower(unaccent(name))
WHERE name IS NOT NULL;