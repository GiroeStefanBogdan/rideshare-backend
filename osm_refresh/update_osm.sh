#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PBF_URL="https://download.geofabrik.de/europe/romania-latest.osm.pbf"
PBF_PATH="$SCRIPT_DIR/romania-latest.osm.pbf"

if [ -f "$REPO_DIR/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    source "$REPO_DIR/.env"
    set +a
fi

: "${POSTGRES_HOST:?POSTGRES_HOST is required}"
: "${POSTGRES_PORT:?POSTGRES_PORT is required}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"

for command_name in flock osm2pgsql psql pg_isready sha256sum wget; do
    command -v "$command_name" >/dev/null 2>&1 || { echo "Missing required command: $command_name" >&2; exit 1; }
done

export PGPASSWORD="$POSTGRES_PASSWORD"
PSQL=(psql -X -v ON_ERROR_STOP=1 -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB")
exec 9>"/tmp/drumbun-osm-${POSTGRES_DB}.lock"
flock -n 9 || { echo "Another OSM refresh is already running." >&2; exit 1; }
pg_isready -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -d "$POSTGRES_DB" -U "$POSTGRES_USER" >/dev/null
wget -O "$PBF_PATH" "$PBF_URL"
"${PSQL[@]}" -c "CREATE SCHEMA IF NOT EXISTS osm_staging"
osm2pgsql --create -O flex -S "$SCRIPT_DIR/import.lua" --slim --cache 4000 \
    -H "$POSTGRES_HOST" -P "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" "$PBF_PATH"
source_sha256="$(sha256sum "$PBF_PATH" | cut -d ' ' -f 1)"
if ! "${PSQL[@]}" -v source_url="$PBF_URL" -v source_sha256="$source_sha256" \
    -v importer_version="$(osm2pgsql --version | head -1)" -v initial=false -f "$SCRIPT_DIR/update.sql"; then
    "${PSQL[@]}" -c "DROP SCHEMA IF EXISTS osm_staging CASCADE"
    "${PSQL[@]}" -v source_url="$PBF_URL" -v source_sha256="$source_sha256" \
        -v importer_version="$(osm2pgsql --version | head -1)" -c "
            INSERT INTO osm_import_runs(source_url, source_sha256, importer_version, status, finished_at,
                                        failure_message)
            VALUES (:'source_url', :'source_sha256', :'importer_version', 'FAILED', CURRENT_TIMESTAMP,
                    'Reconciliation failed; see refresh job logs')"
    exit 1
fi
