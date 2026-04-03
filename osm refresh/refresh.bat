osm2pgsql --create --output=flex --style=full-flex.lua ^
  --database=your_db --username=postgres ^
  romania-latest.osm.pbf

psql -d your_db -f 01_stage_prepare.sql
psql -d your_db -f 02_merge.sql
psql -d your_db -f 03_search.sql