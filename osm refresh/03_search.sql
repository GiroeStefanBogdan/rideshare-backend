-- =====================
-- ADMIN PRIORITY
-- =====================
UPDATE admin_units
SET priority =
    CASE type
        WHEN 'city' THEN 100
        WHEN 'town' THEN 80
        WHEN 'village' THEN 50
        ELSE 10
    END
    + LEAST(COALESCE(population,0)/10000, 50);

-- =====================
-- ADMIN SEARCH
-- =====================
DROP TABLE IF EXISTS admin_search;

CREATE TABLE admin_search AS
SELECT
    id,
    name,
    type,
    parent_id,
    priority,
    latitude,
    longitude,
    lower(unaccent(name)) AS search_text
FROM admin_units;

CREATE INDEX admin_search_trgm_idx
ON admin_search USING gin (search_text gin_trgm_ops);

-- =====================
-- STREET SEARCH
-- =====================
DROP TABLE IF EXISTS street_search;

CREATE TABLE street_search AS
SELECT
    MIN(id) AS id,
    location_id,
    name_norm,
    ANY_VALUE(name) AS name,
    ST_PointOnSurface(ST_Union(geom)) AS center
FROM streets
WHERE name IS NOT NULL
GROUP BY location_id, name_norm;

ALTER TABLE street_search
ADD COLUMN latitude double precision,
ADD COLUMN longitude double precision;

UPDATE street_search
SET
    latitude = ST_Y(center),
    longitude = ST_X(center);

ALTER TABLE street_search ADD COLUMN search_text text;

UPDATE street_search
SET search_text = lower(unaccent(name));

CREATE INDEX street_search_trgm_idx
ON street_search USING gin (search_text gin_trgm_ops);