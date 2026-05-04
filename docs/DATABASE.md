# Database

## Stack

- **PostgreSQL** with the **PostGIS** extension
- Location data is sourced from OpenStreetMap (OSM)
- Schema migrations are managed by **Flyway**

---

## Flyway Migrations

Migration files live in `src/main/resources/db/migration/`.

### Naming convention

```
V{version}__{description}.sql
```

- Version is a sequential integer: `V1__`, `V2__`, `V3__`, etc.
- Description uses underscores, no spaces: `add_review_table`, not `add review table`
- Two underscores between version and description

**Examples:**
```
V1__init_schema.sql
V2__add_postGIS_extension.sql
V3__add_ride_stops.sql
V4__add_user_reviews.sql
```

### Rules

- **Never modify a migration that has already been applied.** Flyway checksums applied migrations — any change will break the build.
- New schema changes always go in a new migration file with the next version number.
- Prefer additive migrations (new tables, new columns) over destructive ones (drops, renames). If a destructive migration is necessary, discuss first.
- Always check `./mvnw flyway:info` before creating a new migration to confirm the current version.

---

## PostGIS

- Geometry columns use `geography` type (not `geometry`) for distance calculations in metres
- OSM-sourced data is loaded via migration scripts — do not hand-write location seed data
- Native SQL queries involving spatial functions go in a `*Impl` repository class, not in JPQL

---

## Useful Commands

```bash
./mvnw flyway:migrate    # apply all pending migrations
./mvnw flyway:info       # show applied vs pending migrations
./mvnw flyway:validate   # verify checksums of applied migrations match files on disk
```
