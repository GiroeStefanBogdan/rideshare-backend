\set ON_ERROR_STOP on

BEGIN;

SET LOCAL max_parallel_workers_per_gather = 0;

DO $$
BEGIN
    IF NOT pg_try_advisory_xact_lock(hashtext('drumbun-osm-refresh')) THEN
        RAISE EXCEPTION 'Another OSM refresh is already running';
    END IF;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.beautify_name(value TEXT) RETURNS TEXT
LANGUAGE sql IMMUTABLE STRICT AS $$
    WITH cleaned AS (
        SELECT BTRIM(regexp_replace(value, '[[:space:]]+', ' ', 'g')) AS value
    ), positioned AS (
        SELECT value, regexp_instr(value, '[[:alpha:]]') AS letter_position
        FROM cleaned
    )
    SELECT CASE
        WHEN letter_position = 0 THEN value
        ELSE overlay(value PLACING upper(substr(value, letter_position, 1)) FROM letter_position FOR 1)
    END
    FROM positioned
$$;

CREATE TEMP TABLE refresh_context AS
SELECT
    clock_timestamp() AS refreshed_at,
    :'initial'::boolean AS initial_import,
    (SELECT count(*) FROM admin_units WHERE active) AS previous_admin_count,
    (SELECT count(*) FROM streets WHERE active) AS previous_street_count;

INSERT INTO osm_import_runs(source_url, source_sha256, importer_version, status)
VALUES (:'source_url', :'source_sha256', :'importer_version', 'RUNNING')
RETURNING id AS import_run_id \gset

CREATE TEMP TABLE romania_boundary AS
SELECT ST_MakeValid(geom) AS geom
FROM osm_staging.stage_admin
WHERE admin_level = 2
  AND LOWER(unaccent(source_name)) IN ('romania', 'românia')
  AND geom IS NOT NULL
ORDER BY ST_Area(geom) DESC
LIMIT 1;

DO $$
BEGIN
    IF (SELECT count(*) FROM romania_boundary) <> 1
            OR NOT ST_IsValid((SELECT geom FROM romania_boundary)) THEN
        RAISE EXCEPTION 'A valid Romania boundary is required';
    END IF;
END $$;

CREATE TEMP TABLE temp_admin_rows AS
WITH boundaries AS (
    SELECT
        osm_type::char(1) AS osm_type,
        osm_id,
        osm_type || osm_id::text AS candidate_key,
        source_name,
        NULL::text AS place,
        admin_level,
        NULL::integer AS population,
        ST_MakeValid(geom) AS geom
    FROM osm_staging.stage_admin
    WHERE admin_level > 2
      AND ST_Intersects(ST_PointOnSurface(geom), (SELECT geom FROM romania_boundary))
), places AS (
    SELECT
        'N'::char(1) AS osm_type,
        place.osm_id,
        COALESCE(boundary.candidate_key, 'N' || place.osm_id::text) AS candidate_key,
        place.source_name,
        place.place,
        CASE
            WHEN place.place IN ('city', 'town') THEN 6
            WHEN place.place IN ('suburb', 'quarter', 'neighbourhood') THEN 9
            ELSE 8
        END AS admin_level,
        place.population,
        place.geom
    FROM osm_staging.stage_places place
    LEFT JOIN LATERAL (
        SELECT candidate_key
        FROM boundaries boundary
        WHERE LOWER(unaccent(BTRIM(boundary.source_name))) = LOWER(unaccent(BTRIM(place.source_name)))
          AND ST_Covers(boundary.geom, place.geom)
        ORDER BY ST_Area(boundary.geom) ASC
        LIMIT 1
    ) boundary ON TRUE
    WHERE ST_Intersects(place.geom, (SELECT geom FROM romania_boundary))
)
SELECT * FROM boundaries
UNION ALL
SELECT * FROM places;

CREATE TEMP TABLE temp_admin_candidates AS
WITH aggregate_data AS (
    SELECT
        candidate_key,
        MAX(population) AS population,
        MIN(admin_level) AS admin_level,
        bool_or(place = 'city') AS is_city,
        bool_or(place = 'town') AS is_town,
        bool_or(place = 'hamlet') AS is_hamlet,
        bool_or(place = 'locality') AS is_locality,
        bool_or(place = 'suburb') AS is_suburb,
        bool_or(place = 'quarter') AS is_quarter,
        bool_or(place = 'neighbourhood') AS is_neighbourhood
    FROM temp_admin_rows
    GROUP BY candidate_key
), representative AS (
    SELECT DISTINCT ON (candidate_key)
        candidate_key,
        source_name,
        geom
    FROM temp_admin_rows
    ORDER BY candidate_key,
        (GeometryType(geom) IN ('POLYGON', 'MULTIPOLYGON')) DESC,
        ST_Area(geom) DESC
)
SELECT
    aggregate_data.candidate_key,
    pg_temp.beautify_name(representative.source_name) AS display_name,
    representative.source_name,
    LOWER(unaccent(BTRIM(representative.source_name))) AS name_norm,
    CASE
        WHEN aggregate_data.admin_level = 4 THEN 'COUNTY'
        WHEN aggregate_data.is_city THEN 'CITY'
        WHEN aggregate_data.is_town THEN 'TOWN'
        WHEN aggregate_data.is_hamlet THEN 'HAMLET'
        WHEN aggregate_data.is_suburb THEN 'SUBURB'
        WHEN aggregate_data.is_quarter THEN 'QUARTER'
        WHEN aggregate_data.is_neighbourhood THEN 'NEIGHBOURHOOD'
        WHEN aggregate_data.is_locality THEN 'LOCALITY'
        WHEN aggregate_data.admin_level = 6 THEN 'UAT'
        ELSE 'VILLAGE'
    END AS type,
    aggregate_data.admin_level,
    aggregate_data.population,
    representative.geom,
    NULL::bigint AS location_id
FROM aggregate_data
JOIN representative USING (candidate_key);

CREATE INDEX idx_temp_admin_candidates_geom ON temp_admin_candidates USING GIST (geom);
ANALYZE temp_admin_candidates;

UPDATE temp_admin_candidates candidate
SET location_id = source.admin_unit_id
FROM temp_admin_rows row
JOIN osm_location_sources source
  ON source.osm_type = row.osm_type AND source.osm_id = row.osm_id
WHERE row.candidate_key = candidate.candidate_key
  AND source.admin_unit_id IS NOT NULL;

UPDATE temp_admin_candidates candidate
SET location_id = existing.id
FROM admin_units existing
WHERE candidate.location_id IS NULL
  AND existing.osm_id = candidate.candidate_key;

UPDATE temp_admin_candidates candidate
SET location_id = (
    SELECT MIN(existing.id)
    FROM admin_units existing
    WHERE existing.name_norm = candidate.name_norm
      AND existing.type::text = candidate.type
      AND ST_DWithin(existing.geom::geography, candidate.geom::geography, 5000)
      AND NOT EXISTS (
          SELECT FROM temp_admin_candidates claimed
          WHERE claimed.location_id = existing.id
      )
      AND (
          SELECT count(*)
          FROM temp_admin_candidates other
          WHERE other.location_id IS NULL
            AND other.name_norm = existing.name_norm
            AND other.type = existing.type::text
            AND ST_DWithin(existing.geom::geography, other.geom::geography, 5000)
      ) = 1
    HAVING count(*) = 1
)
WHERE candidate.location_id IS NULL;

UPDATE temp_admin_candidates
SET location_id = nextval(pg_get_serial_sequence('admin_units', 'id'))
WHERE location_id IS NULL;

INSERT INTO admin_units(
    id, osm_id, name, source_name, name_norm, type, admin_level, population,
    latitude, longitude, geom, active, last_seen_at, unavailable_since, display_full_name
)
SELECT
    location_id, candidate_key, display_name, source_name, name_norm, type, admin_level, population,
    ST_Y(ST_PointOnSurface(geom)), ST_X(ST_PointOnSurface(geom)), geom, TRUE,
    (SELECT refreshed_at FROM refresh_context), NULL, display_name
FROM temp_admin_candidates
ON CONFLICT (id) DO UPDATE SET
    osm_id = EXCLUDED.osm_id,
    name = EXCLUDED.name,
    source_name = EXCLUDED.source_name,
    name_norm = EXCLUDED.name_norm,
    type = EXCLUDED.type,
    admin_level = EXCLUDED.admin_level,
    population = EXCLUDED.population,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    geom = EXCLUDED.geom,
    active = TRUE,
    last_seen_at = EXCLUDED.last_seen_at,
    unavailable_since = NULL;

INSERT INTO osm_location_sources(osm_type, osm_id, admin_unit_id, last_seen_at)
SELECT DISTINCT row.osm_type, row.osm_id, candidate.location_id, (SELECT refreshed_at FROM refresh_context)
FROM temp_admin_rows row
JOIN temp_admin_candidates candidate USING (candidate_key)
ON CONFLICT (osm_type, osm_id) WHERE admin_unit_id IS NOT NULL DO UPDATE SET
    admin_unit_id = EXCLUDED.admin_unit_id,
    last_seen_at = EXCLUDED.last_seen_at;

UPDATE admin_units child
SET parent_id = (
    SELECT parent.id
    FROM admin_units parent
    WHERE parent.id <> child.id
      AND parent.active
      AND parent.admin_level < child.admin_level
      AND GeometryType(parent.geom) IN ('POLYGON', 'MULTIPOLYGON')
      AND ST_Covers(parent.geom, ST_PointOnSurface(child.geom))
    ORDER BY parent.admin_level DESC, ST_Area(parent.geom) ASC, parent.id
    LIMIT 1
)
WHERE child.active;

WITH RECURSIVE paths AS (
    SELECT id AS base_id, name::text AS path, parent_id
    FROM admin_units
    WHERE active
    UNION ALL
    SELECT paths.base_id, paths.path || ', ' || parent.name, parent.parent_id
    FROM paths
    JOIN admin_units parent ON parent.id = paths.parent_id
), resolved AS (
    SELECT base_id, path
    FROM paths
    WHERE parent_id IS NULL
)
UPDATE admin_units unit
SET display_full_name = resolved.path
FROM resolved
WHERE unit.id = resolved.base_id;

CREATE TEMP TABLE temp_street_segments AS
SELECT
    street.osm_id,
    unit.id AS location_id,
    street.source_name,
    LOWER(unaccent(BTRIM(street.source_name))) AS name_norm,
    ST_CollectionExtract(ST_Intersection(ST_MakeValid(street.geom), unit.geom), 2) AS geom
FROM osm_staging.stage_streets street
JOIN LATERAL (
    SELECT candidate.location_id AS id, candidate.geom
    FROM temp_admin_candidates candidate
    WHERE candidate.type <> 'COUNTY'
      AND GeometryType(candidate.geom) IN ('POLYGON', 'MULTIPOLYGON')
      AND ST_Intersects(street.geom, candidate.geom)
    ORDER BY candidate.admin_level DESC, ST_Area(candidate.geom) ASC, candidate.location_id
    LIMIT 1
) unit ON TRUE;

DELETE FROM temp_street_segments WHERE ST_IsEmpty(geom);

CREATE TEMP TABLE temp_street_candidates AS
SELECT
    location_id,
    name_norm,
    pg_temp.beautify_name(MAX(source_name)) AS display_name,
    MAX(source_name) AS source_name,
    ST_Multi(ST_LineMerge(ST_Union(geom))) AS geom,
    NULL::bigint AS street_id
FROM temp_street_segments
GROUP BY location_id, name_norm;

UPDATE temp_street_candidates candidate
SET street_id = existing.id
FROM streets existing
WHERE existing.location_id = candidate.location_id
  AND existing.name_norm = candidate.name_norm;

UPDATE temp_street_candidates candidate
SET street_id = source.street_id
FROM temp_street_segments segment
JOIN osm_location_sources source ON source.osm_type = 'W' AND source.osm_id = segment.osm_id
JOIN streets existing ON existing.id = source.street_id
WHERE candidate.street_id IS NULL
  AND segment.location_id = candidate.location_id
  AND segment.name_norm = candidate.name_norm
  AND existing.location_id = candidate.location_id;

UPDATE temp_street_candidates
SET street_id = nextval(pg_get_serial_sequence('streets', 'id'))
WHERE street_id IS NULL;

INSERT INTO streets(
    id, location_id, name, source_name, name_norm, full_name, display_full_name,
    latitude, longitude, geom, active, last_seen_at, unavailable_since
)
SELECT
    candidate.street_id, candidate.location_id, candidate.display_name, candidate.source_name,
    candidate.name_norm,
    LOWER(unaccent(candidate.display_name || ', ' || unit.name)),
    candidate.display_name || ', ' || unit.name,
    ST_Y(ST_PointOnSurface(candidate.geom)), ST_X(ST_PointOnSurface(candidate.geom)), candidate.geom,
    TRUE, (SELECT refreshed_at FROM refresh_context), NULL
FROM temp_street_candidates candidate
JOIN admin_units unit ON unit.id = candidate.location_id
ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    name = EXCLUDED.name,
    source_name = EXCLUDED.source_name,
    name_norm = EXCLUDED.name_norm,
    full_name = EXCLUDED.full_name,
    display_full_name = EXCLUDED.display_full_name,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    geom = EXCLUDED.geom,
    active = TRUE,
    last_seen_at = EXCLUDED.last_seen_at,
    unavailable_since = NULL;

INSERT INTO osm_location_sources(osm_type, osm_id, street_id, last_seen_at)
SELECT DISTINCT 'W', segment.osm_id, candidate.street_id, (SELECT refreshed_at FROM refresh_context)
FROM temp_street_segments segment
JOIN temp_street_candidates candidate
  ON candidate.location_id = segment.location_id AND candidate.name_norm = segment.name_norm
ON CONFLICT (osm_type, osm_id, street_id) WHERE street_id IS NOT NULL DO UPDATE SET
    last_seen_at = EXCLUDED.last_seen_at;

UPDATE admin_units unit
SET active = FALSE,
    unavailable_since = COALESCE(unit.unavailable_since, (SELECT refreshed_at FROM refresh_context))
WHERE unit.active
  AND NOT EXISTS (
      SELECT FROM osm_location_sources source
      WHERE source.admin_unit_id = unit.id
        AND source.last_seen_at = (SELECT refreshed_at FROM refresh_context)
  );

UPDATE streets street
SET active = FALSE,
    unavailable_since = COALESCE(street.unavailable_since, (SELECT refreshed_at FROM refresh_context))
WHERE street.active
  AND NOT EXISTS (
      SELECT FROM osm_location_sources source
      WHERE source.street_id = street.id
        AND source.last_seen_at = (SELECT refreshed_at FROM refresh_context)
  );

DO $$
DECLARE
    old_admin_count bigint := (SELECT previous_admin_count FROM refresh_context);
    new_admin_count bigint := (SELECT count(*) FROM temp_admin_candidates);
    old_street_count bigint := (SELECT previous_street_count FROM refresh_context);
    new_street_count bigint := (SELECT count(*) FROM temp_street_candidates);
BEGIN
    IF EXISTS (SELECT FROM temp_admin_candidates WHERE source_name = '' OR geom IS NULL OR NOT ST_IsValid(geom))
            OR EXISTS (SELECT FROM temp_street_candidates WHERE source_name = '' OR geom IS NULL OR NOT ST_IsValid(geom)) THEN
        RAISE EXCEPTION 'Selectable locations contain blank names or invalid geometries';
    END IF;
    IF NOT (SELECT initial_import FROM refresh_context)
            AND old_admin_count > 0 AND new_admin_count < old_admin_count * 0.90 THEN
        RAISE EXCEPTION 'Administrative-unit count dropped by more than 10%%';
    END IF;
    IF NOT (SELECT initial_import FROM refresh_context)
            AND old_street_count > 0 AND new_street_count < old_street_count * 0.85 THEN
        RAISE EXCEPTION 'Street count dropped by more than 15%%';
    END IF;
    IF (SELECT count(*) FROM osm_staging.stage_streets) > 0 AND (
        SELECT count(*)
        FROM osm_staging.stage_streets source
        WHERE NOT EXISTS (SELECT FROM temp_street_segments assigned WHERE assigned.osm_id = source.osm_id)
    ) > (SELECT count(*) FROM osm_staging.stage_streets) * 0.05 THEN
        RAISE EXCEPTION 'More than 5%% of eligible street segments are unassigned';
    END IF;
    IF (
        SELECT count(*) FILTER (WHERE parent_id IS NULL) > count(*) * 0.02
        FROM admin_units
        WHERE active AND type <> 'COUNTY' AND GeometryType(geom) IN ('POLYGON', 'MULTIPOLYGON')
    ) THEN
        RAISE EXCEPTION 'More than 2%% of bounded administrative units lack a parent';
    END IF;
END $$;

UPDATE osm_import_runs
SET status = 'SUCCEEDED',
    finished_at = clock_timestamp(),
    metrics = jsonb_build_object(
        'admin_units', (SELECT count(*) FROM temp_admin_candidates),
        'streets', (SELECT count(*) FROM temp_street_candidates),
        'unassigned_street_segments', (
            SELECT count(*) FROM osm_staging.stage_streets source
            WHERE NOT EXISTS (SELECT FROM temp_street_segments assigned WHERE assigned.osm_id = source.osm_id)
        )
    )
WHERE id = :import_run_id;

DROP SCHEMA osm_staging CASCADE;
COMMIT;
