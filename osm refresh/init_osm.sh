#!/bin/bash
# init_osm.sh

DB_NAME="your_db_name"
DB_USER="postgres"
DB_PASS="your_password"
PBF_URL="https://download.geofabrik.de/europe/romania-latest.osm.pbf"
PBF_FILE="romania-latest.osm.pbf"

export PGPASSWORD=$DB_PASS

echo "Downloading latest Romania OSM data..."
wget -O $PBF_FILE $PBF_URL

echo "Running osm2pgsql (Initial Staging)..."
osm2pgsql -O flex -S import.lua --slim --cache 4000 \
    -d "postgresql://$DB_USER:$DB_PASS@localhost:5432/$DB_NAME" $PBF_FILE

echo "Running Initial SQL Processing..."
psql -h localhost -U $DB_USER -d $DB_NAME -f init.sql

echo "Initial Import Complete!"