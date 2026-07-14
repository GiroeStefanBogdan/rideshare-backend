# Database

## Stack

PostgreSQL with PostGIS. Location data comes from OpenStreetMap. Flyway manages schema changes.

## Migrations

Files live in `src/main/resources/db/migration/` and use:

```text
V{version}__{description}.sql
```

Versions are sequential integers; descriptions use underscores.

Never edit an applied migration. Before adding one, run `./mvnw flyway:info`. Use a new additive migration for every schema change.

## PostGIS

- Use `geography` for distance calculations in metres.
- Keep spatial/native SQL in repository `*Impl` classes.
- Do not hand-write OSM seed data in application code.

## JPA

Use lazy associations, explicit join columns, and string enum storage for new enum fields. Keep database constraints aligned with domain invariants, especially ride-stop ordering, seat bounds, and booking references.

## Commands

```bash
./mvnw flyway:info
./mvnw flyway:migrate
./mvnw flyway:validate
```
