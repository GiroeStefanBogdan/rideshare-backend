-- init.sql

-- 1. Clear existing data to ensure a clean slate
TRUNCATE TABLE streets, admin_units RESTART IDENTITY CASCADE;

-- (Optional but recommended: drop constraints if re-running this script)
ALTER TABLE admin_units DROP CONSTRAINT IF EXISTS admin_units_osm_id_key;
ALTER TABLE streets DROP CONSTRAINT IF EXISTS streets_loc_name_key;

-- 2. Populate admin_units (Deduplicating polygon/point records in OSM)
-- A. Extract the Master Boundary to clip foreign data
CREATE TEMP TABLE romania_boundary AS
SELECT geom FROM stage_admin
WHERE admin_level = 2 AND name IN ('România', 'Romania')
LIMIT 1;

-- B. Gather, Filter, and Spatially Cluster Admin Units
WITH filtered_admins AS (
    -- Get Polygons (Level 4, 6, 8, 9, 10). Reject anything outside Romania.
    SELECT
        osm_type || osm_id::text AS o_id,
        name,
        admin_level,
        NULL::int AS population,
        geom
    FROM stage_admin
    WHERE admin_level > 2
      AND ST_Intersects(ST_Centroid(geom), (SELECT geom FROM romania_boundary))

    UNION ALL

    -- Get Points. Reject anything outside Romania.
    SELECT
        'N' || osm_id::text AS o_id,
        name,
        CASE WHEN place IN ('city', 'town') THEN 6 ELSE 8 END AS admin_level,
        population,
        geom
    FROM stage_places
    WHERE ST_Intersects(geom, (SELECT geom FROM romania_boundary))
),
clustered_admins AS (
    -- ST_ClusterDBSCAN groups geometries that physically intersect (eps:=0)
    -- We partition by name, meaning it only groups them if they ALSO share the exact name.
    SELECT *,
           ST_ClusterDBSCAN(geom, 0, 1) OVER (PARTITION BY LOWER(unaccent(name))) AS cluster_id
    FROM filtered_admins
),
aggregated_admins AS (
    -- For each cluster, find the highest admin level and max population
    SELECT *,
           MAX(population) OVER(PARTITION BY LOWER(unaccent(name)), cluster_id) as max_pop,
           MAX(admin_level) OVER(PARTITION BY LOWER(unaccent(name)), cluster_id) as max_al,
           -- Rank them so we can pick the best geometry to represent the merged cluster
           ROW_NUMBER() OVER(
               PARTITION BY LOWER(unaccent(name)), cluster_id
               ORDER BY
                   (ST_GeometryType(geom) IN ('ST_Polygon', 'ST_MultiPolygon')) DESC, -- Prefer Polygons over Points
                   ST_Area(geom) DESC -- Prefer the largest polygon available
           ) as rn
    FROM clustered_admins
)
-- C. Insert the aggregated master records
INSERT INTO admin_units (osm_id, name, type, admin_level, population, geom)
SELECT
    o_id,
    name,
    CASE
        WHEN max_al = 4 THEN 'COUNTY'
        WHEN max_al = 6 THEN 'UAT'
        ELSE 'VILLAGE'
    END as type,
    max_al,
    max_pop,
    geom
FROM aggregated_admins
WHERE rn = 1;

-- D. Clean up the boundary table
DROP TABLE romania_boundary;

-- 3. Calculate Centroids for admin_units
UPDATE admin_units
SET latitude = ST_Y(ST_Centroid(geom)),
    longitude = ST_X(ST_Centroid(geom));

-- 4. Build the Hierarchy (Establish parent_id)
UPDATE admin_units child
SET parent_id = parent.id
FROM admin_units parent
WHERE child.id != parent.id
  AND child.admin_level > parent.admin_level
  AND ST_GeometryType(parent.geom) IN ('ST_Polygon', 'ST_MultiPolygon')
  AND ST_Intersects(ST_Centroid(child.geom), parent.geom);

-- 5. Create a Temp Table for Raw Street Segments
CREATE TEMP TABLE temp_raw_streets AS
SELECT name, LOWER(unaccent(name)) as name_norm, geom, NULL::int as location_id
FROM stage_streets;

CREATE INDEX idx_temp_raw_geom ON temp_raw_streets USING GIST(geom);

-- 6. Street Mapping Rule 1: Spatial Match to Polygon
UPDATE temp_raw_streets s
SET location_id = a.id
FROM admin_units a
WHERE a.admin_level > 4
  AND ST_GeometryType(a.geom) IN ('ST_Polygon', 'ST_MultiPolygon')
  AND ST_Intersects(s.geom, a.geom)
  AND s.location_id IS NULL;

-- 7. Street Mapping Rule 2: Spatial Fallback to Nearest Point
UPDATE temp_raw_streets s
SET location_id = (
    SELECT a.id FROM admin_units a
    WHERE a.admin_level > 4 AND ST_GeometryType(a.geom) = 'ST_Point'
    ORDER BY s.geom <-> a.geom LIMIT 1
)
WHERE s.location_id IS NULL;

-- 8. Group, Merge Geometries, and Insert into Final Streets Table
-- FIX APPLIED: Group strictly by location_id and name_norm, using MAX(name) to handle case variations
INSERT INTO streets (location_id, name, name_norm, latitude, longitude, geom)
SELECT
    location_id,
    MAX(name),
    name_norm,
    ST_Y(ST_Centroid(ST_Multi(ST_LineMerge(ST_Union(geom))))),
    ST_X(ST_Centroid(ST_Multi(ST_LineMerge(ST_Union(geom))))),
    ST_Multi(ST_LineMerge(ST_Union(geom)))
FROM temp_raw_streets
WHERE location_id IS NOT NULL
GROUP BY location_id, name_norm;

-- 9. Generate the Recursive full_name for Streets
WITH RECURSIVE admin_tree AS (
    SELECT id as base_id, id as current_id, name::text as path, parent_id
    FROM admin_units
    UNION ALL
    SELECT t.base_id, p.id, t.path || ', ' || p.name, p.parent_id
    FROM admin_tree t
    JOIN admin_units p ON t.parent_id = p.id
),
resolved_paths AS (
    SELECT base_id, path FROM admin_tree WHERE parent_id IS NULL
)
UPDATE streets s
SET full_name = LOWER(unaccent(s.name || ', ' || rp.path))
FROM resolved_paths rp
WHERE s.location_id = rp.base_id;

-- 10. Apply Strict Unique Constraints (Prepares DB for Future Updates)
ALTER TABLE admin_units ADD CONSTRAINT admin_units_osm_id_key UNIQUE (osm_id);
ALTER TABLE streets ADD CONSTRAINT streets_loc_name_key UNIQUE (location_id, name_norm);

-- 11. Create pg_trgm Search Optimizations
CREATE INDEX IF NOT EXISTS idx_streets_name_trgm ON streets USING gin (name_norm gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_streets_fullname_trgm ON streets USING gin (full_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_streets_location_id ON streets(location_id);
CREATE INDEX IF NOT EXISTS idx_admin_parent_id ON admin_units(parent_id);

-- 12. Cleanup Staging & Temp Tables
DROP TABLE temp_raw_streets;
DROP TABLE stage_admin;
DROP TABLE stage_places;
DROP TABLE stage_streets;