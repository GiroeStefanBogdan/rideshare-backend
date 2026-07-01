#!/usr/bin/env bash
# init_osm.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ -f "$REPO_DIR/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    source "$REPO_DIR/.env"
    set +a
fi

DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5432}"
DB_NAME="${POSTGRES_DB:-aries}"
DB_USER="${POSTGRES_USER:-postgres}"
DB_PASS="${POSTGRES_PASSWORD:-admin}"
PBF_URL="https://download.geofabrik.de/europe/romania-latest.osm.pbf"
PBF_FILE="romania-latest.osm.pbf"
PBF_PATH="$SCRIPT_DIR/$PBF_FILE"
IMPORT_SCRIPT="$SCRIPT_DIR/import.lua"
INIT_SQL="$SCRIPT_DIR/init.sql"

require_command() {
    local command_name="$1"

    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "Missing required command: $command_name" >&2
        exit 1
    fi
}

require_command osm2pgsql
require_command psql
require_command pg_isready

if [ ! -f "$PBF_PATH" ]; then
    require_command wget
    echo "Downloading latest Romania OSM data..."
    wget -O "$PBF_PATH" "$PBF_URL"
fi

export PGPASSWORD="$DB_PASS"

if ! pg_isready -h "$DB_HOST" -p "$DB_PORT" -d "$DB_NAME" -U "$DB_USER" >/dev/null; then
    echo "PostgreSQL is not ready at $DB_HOST:$DB_PORT for database '$DB_NAME' and user '$DB_USER'." >&2
    exit 1
fi

echo "Running osm2pgsql (Initial Staging)..."
osm2pgsql -O flex -S "$IMPORT_SCRIPT" --slim --cache 4000 \
    -d "postgresql://$DB_USER:$DB_PASS@$DB_HOST:$DB_PORT/$DB_NAME" "$PBF_PATH"

echo "Running Initial SQL Processing..."
psql -v ON_ERROR_STOP=1 -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$INIT_SQL"

echo "Initial Import Complete!"
