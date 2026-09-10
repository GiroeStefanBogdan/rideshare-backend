CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

ALTER TABLE admin_units ADD COLUMN IF NOT EXISTS source_name VARCHAR(100);
ALTER TABLE admin_units ADD COLUMN IF NOT EXISTS name_norm VARCHAR(100);
ALTER TABLE admin_units ADD COLUMN IF NOT EXISTS display_full_name TEXT;
ALTER TABLE admin_units ADD COLUMN IF NOT EXISTS admin_level INTEGER;
ALTER TABLE admin_units ADD COLUMN IF NOT EXISTS geom geometry(Geometry, 4326);
ALTER TABLE admin_units ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE admin_units ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMPTZ;
ALTER TABLE admin_units ADD COLUMN IF NOT EXISTS unavailable_since TIMESTAMPTZ;

ALTER TABLE streets ADD COLUMN IF NOT EXISTS source_name VARCHAR(150);
ALTER TABLE streets ADD COLUMN IF NOT EXISTS display_full_name TEXT;
ALTER TABLE streets ADD COLUMN IF NOT EXISTS geom geometry(MultiLineString, 4326);
ALTER TABLE streets ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE streets ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMPTZ;
ALTER TABLE streets ADD COLUMN IF NOT EXISTS unavailable_since TIMESTAMPTZ;

UPDATE admin_units
SET source_name = COALESCE(source_name, name),
    name_norm = COALESCE(name_norm, LOWER(unaccent(BTRIM(name)))),
    display_full_name = COALESCE(display_full_name, name),
    last_seen_at = COALESCE(last_seen_at, CURRENT_TIMESTAMP);

UPDATE streets
SET source_name = COALESCE(source_name, name),
    display_full_name = COALESCE(display_full_name, name),
    last_seen_at = COALESCE(last_seen_at, CURRENT_TIMESTAMP);

ALTER TABLE admin_units ALTER COLUMN source_name SET NOT NULL;
ALTER TABLE admin_units ALTER COLUMN name_norm SET NOT NULL;
ALTER TABLE streets ALTER COLUMN source_name SET NOT NULL;

CREATE TABLE IF NOT EXISTS osm_location_sources (
    id BIGSERIAL PRIMARY KEY,
    osm_type CHAR(1) NOT NULL,
    osm_id BIGINT NOT NULL,
    admin_unit_id BIGINT REFERENCES admin_units(id),
    street_id BIGINT REFERENCES streets(id),
    last_seen_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT osm_location_sources_one_target CHECK (
        (admin_unit_id IS NOT NULL)::INTEGER + (street_id IS NOT NULL)::INTEGER = 1
    )
);

CREATE TABLE IF NOT EXISTS osm_import_runs (
    id BIGSERIAL PRIMARY KEY,
    source_url TEXT NOT NULL,
    source_sha256 CHAR(64) NOT NULL,
    importer_version TEXT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    metrics JSONB NOT NULL DEFAULT '{}'::jsonb,
    failure_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_admin_units_name_norm_trgm
    ON admin_units USING GIN (name_norm gin_trgm_ops) WHERE active;
CREATE INDEX IF NOT EXISTS idx_admin_units_geom ON admin_units USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_streets_name_norm_trgm
    ON streets USING GIN (name_norm gin_trgm_ops) WHERE active;
CREATE INDEX IF NOT EXISTS idx_streets_geom ON streets USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_osm_sources_admin ON osm_location_sources(admin_unit_id);
CREATE INDEX IF NOT EXISTS idx_osm_sources_street ON osm_location_sources(street_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_osm_sources_admin_identity
    ON osm_location_sources(osm_type, osm_id) WHERE admin_unit_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_osm_sources_street_identity
    ON osm_location_sources(osm_type, osm_id, street_id) WHERE street_id IS NOT NULL;
