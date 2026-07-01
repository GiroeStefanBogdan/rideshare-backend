-- update.sql

-- 1. Extract the Master Boundary to clip foreign data
CREATE TEMP TABLE romania_boundary AS
SELECT geom FROM stage_admin
WHERE admin_level = 2 AND name IN ('România', 'Romania')
LIMIT 1;

-- 2. Gather, Filter, and Spatially Cluster Admin Units
WITH filtered_admins AS (
    SELECT osm_type || osm_id::text AS o_id, name, admin_level, NULL::int AS population, geom
    FROM stage_admin
    WHERE admin_level > 2 AND ST_Intersects(ST_Centroid(geom), (SELECT geom FROM romania_boundary))
    UNION ALL
    SELECT 'N' || osm_id::text AS o_id, name, CASE WHEN place IN ('city', 'town') THEN 6 ELSE 8 END AS admin_level, population, geom
    FROM stage_places
    WHERE ST_Intersects(geom, (SELECT geom FROM romania_boundary))
),
clustered_admins AS (
    SELECT *, ST_ClusterDBSCAN(geom, 0, 1) OVER (PARTITION BY LOWER(unaccent(name))) AS cluster_id
    FROM filtered_admins
),
aggregated_admins AS (
    SELECT *,
           MAX(population) OVER(PARTITION BY LOWER(unaccent(name)), cluster_id) as max_pop,
           MAX(admin_level) OVER(PARTITION BY LOWER(unaccent(name)), cluster_id) as max_al,
           ROW_NUMBER() OVER(
               PARTITION BY LOWER(unaccent(name)), cluster_id
               ORDER BY (ST_GeometryType(geom) IN ('ST_Polygon', 'ST_MultiPolygon')) DESC, ST_Area(geom) DESC
           ) as rn
    FROM clustered_admins
)
-- 3. Upsert Admin Units (Preserves existing database IDs)
INSERT INTO admin_units (osm_id, name, type, admin_level, population, geom)
SELECT
    o_id, name,
    CASE WHEN max_al = 4 THEN 'COUNTY' WHEN max_al = 6 THEN 'UAT' ELSE 'VILLAGE' END as type,
    max_al, max_pop, geom
FROM aggregated_admins
WHERE rn = 1
ON CONFLICT (osm_id) DO UPDATE
SET name = EXCLUDED.name,
    type = EXCLUDED.type,
    admin_level = EXCLUDED.admin_level,
    population = EXCLUDED.population,
    geom = EXCLUDED.geom;

-- 4. Recalculate Centroids & Hierarchy
UPDATE admin_units SET latitude = ST_Y(ST_Centroid(geom)), longitude = ST_X(ST_Centroid(geom));

UPDATE admin_units child
SET parent_id = parent.id
FROM admin_units parent
WHERE child.id != parent.id AND child.admin_level > parent.admin_level
  AND ST_GeometryType(parent.geom) IN ('ST_Polygon', 'ST_MultiPolygon')
  AND ST_Intersects(ST_Centroid(child.geom), parent.geom);

-- 5. Process New Streets into Temp Table
CREATE TEMP TABLE temp_raw_streets AS
SELECT name, LOWER(unaccent(name)) as name_norm, geom, NULL::int as location_id
FROM stage_streets;

CREATE INDEX idx_temp_raw_geom ON temp_raw_streets USING GIST(geom);

-- 6. Map Streets to Locations
UPDATE temp_raw_streets s SET location_id = a.id
FROM admin_units a WHERE a.admin_level > 4 AND ST_GeometryType(a.geom) IN ('ST_Polygon', 'ST_MultiPolygon') AND ST_Intersects(s.geom, a.geom) AND s.location_id IS NULL;

UPDATE temp_raw_streets s SET location_id = (
    SELECT a.id FROM admin_units a WHERE a.admin_level > 4 AND ST_GeometryType(a.geom) = 'ST_Point' ORDER BY s.geom <-> a.geom LIMIT 1
) WHERE s.location_id IS NULL;

-- 7. Upsert Streets (Handles Capitalization Fix & Preserves IDs)
WITH merged_streets AS (
    SELECT
        location_id,
        MAX(name) as name, -- Picks one capitalization variant
        name_norm,
        ST_Y(ST_Centroid(ST_Multi(ST_LineMerge(ST_Union(geom))))) as latitude,
        ST_X(ST_Centroid(ST_Multi(ST_LineMerge(ST_Union(geom))))) as longitude,
        ST_Multi(ST_LineMerge(ST_Union(geom))) as geom
    FROM temp_raw_streets
    WHERE location_id IS NOT NULL
    GROUP BY location_id, name_norm -- Groups strictly by the unique constraint
)
INSERT INTO streets (location_id, name, name_norm, latitude, longitude, geom)
SELECT location_id, name, name_norm, latitude, longitude, geom
FROM merged_streets
ON CONFLICT (location_id, name_norm) DO UPDATE
SET name = EXCLUDED.name,
    geom = EXCLUDED.geom,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude;

-- 8. Re-generate Full Names
WITH RECURSIVE admin_tree AS (
    SELECT id as base_id, id as current_id, name::text as path, parent_id FROM admin_units
    UNION ALL
    SELECT t.base_id, p.id, t.path || ', ' || p.name, p.parent_id FROM admin_tree t JOIN admin_units p ON t.parent_id = p.id
),
resolved_paths AS (SELECT base_id, path FROM admin_tree WHERE parent_id IS NULL)
UPDATE streets s
SET full_name = LOWER(unaccent(s.name || ', ' || rp.path))
FROM resolved_paths rp
WHERE s.location_id = rp.base_id;

-- 9. Cleanup
DROP TABLE romania_boundary;
DROP TABLE temp_raw_streets;
DROP TABLE stage_admin;
DROP TABLE stage_places;
DROP TABLE stage_streets;