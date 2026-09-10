#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PBF_PATH=""

if [ "${1:-}" = "--pbf" ] && [ -n "${2:-}" ]; then
    PBF_PATH="$(cd "$(dirname "$2")" && pwd)/$(basename "$2")"
elif [ "$#" -ne 0 ]; then
    echo "Usage: $0 [--pbf /path/to/romania.osm.pbf]" >&2
    exit 2
fi

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

existing_rows="$("${PSQL[@]}" -Atc "SELECT (SELECT count(*) FROM admin_units) + (SELECT count(*) FROM streets) + (SELECT count(*) FROM ride)")"
if [ "$existing_rows" -ne 0 ]; then
    echo "Initialization requires empty admin_units, streets, and ride tables." >&2
    exit 1
fi

if [ -z "$PBF_PATH" ]; then
    PBF_PATH="$SCRIPT_DIR/romania-latest.osm.pbf"
    wget -O "$PBF_PATH" "https://download.geofabrik.de/europe/romania-latest.osm.pbf"
fi

"${PSQL[@]}" -c "CREATE SCHEMA IF NOT EXISTS osm_staging"
osm2pgsql --create -O flex -S "$SCRIPT_DIR/import.lua" --slim --cache 4000 \
    -H "$POSTGRES_HOST" -P "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" "$PBF_PATH"
source_sha256="$(sha256sum "$PBF_PATH" | cut -d ' ' -f 1)"
"${PSQL[@]}" -v source_url="$PBF_PATH" -v source_sha256="$source_sha256" \
    -v importer_version="$(osm2pgsql --version | head -1)" -v initial=true -f "$SCRIPT_DIR/init.sql"
