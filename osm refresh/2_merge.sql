-- =====================
-- ADMIN MERGE
-- =====================

-- update existing
UPDATE admin_units a
SET
    latitude = s.latitude,
    longitude = s.longitude,
    population = s.population
FROM admin_units_stage s
WHERE a.osm_id = s.osm_id;

-- insert new
INSERT INTO admin_units (osm_id, name, type, parent_id, latitude, longitude, population)
SELECT
    s.osm_id, s.name, s.type, NULL, s.latitude, s.longitude, s.population
FROM admin_units_stage s
WHERE NOT EXISTS (
    SELECT 1 FROM admin_units a WHERE a.osm_id = s.osm_id
);

-- =====================
-- STREETS MERGE
-- =====================

INSERT INTO streets (osm_id, name, alt_name, geom)
SELECT
    osm_id, name, alt_name, geom
FROM streets_stage s
WHERE NOT EXISTS (
    SELECT 1 FROM streets st WHERE st.osm_id = s.osm_id
);