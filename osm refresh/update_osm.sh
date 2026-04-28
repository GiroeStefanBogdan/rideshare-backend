#!/bin/bash
# update_osm.sh

DB_NAME="your_db_name"
DB_USER="postgres"
DB_PASS="your_password"
PBF_URL="https://download.geofabrik.de/europe/romania-latest.osm.pbf"
PBF_FILE="romania-latest.osm.pbf"

export PGPASSWORD=$DB_PASS

echo "Downloading map updates..."
# Use -N to only download if the file on Geofabrik is newer than our local copy
wget -N $PBF_URL

echo "Running osm2pgsql (Overwriting Staging Tables)..."
# This safely overwrites stage_admin, stage_places, stage_streets with fresh data
osm2pgsql -O flex -S import.lua --slim --cache 4000 \
    -d "postgresql://$DB_USER:$DB_PASS@localhost:5432/$DB_NAME" $PBF_FILE

echo "Running Update SQL Processing (Upserts)..."
psql -h localhost -U $DB_USER -d $DB_NAME -f update.sql

echo "Update Complete!"