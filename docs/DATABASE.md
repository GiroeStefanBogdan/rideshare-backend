# Database & Migrations

PostgreSQL with PostGIS extensions. Schema is versioned using Flyway.

## Migrations
- Location: `src/main/resources/db/migration/`
- Naming format: `V{sequential_integer}__{snake_case_description}.sql` (e.g., `V12__add_ride_indexes.sql`).
- **All migrations are additive.** Never edit, rename, or delete an applied migration.
- Always check migration status before adding a new file: `./mvnw flyway:info`.
- Flyway exclusively owns permanent tables, columns, extensions, constraints, and indexes. Import jobs may own
  temporary staging structures but must not evolve the permanent schema.

## OSM Catalog Refresh
- Full Romania refreshes run monthly, with a database advisory lock allowing only one refresh at a time.
- Each generation records its source, checksum, importer version, timing, validation metrics, and outcome.
- Failed generations never replace the active catalog. Their report and metadata remain, while staging data is
  removed unless explicitly retained temporarily for debugging.

## PostGIS & Spatial Logic
- Use the `geography` type for distance calculations in meters.
- Spatial calculations and native SQL belong in `repository/*Impl` classes.
- Never hardcode OpenStreetMap seed data in Java code.

## JPA Rules
- Lazy loading by default (`FetchType.LAZY`) on all associations.
- Owning side specifies `@JoinColumn`; inverse collection side uses `mappedBy`.
- Store enums as strings: `@Enumerated(EnumType.STRING)`.
- Use `@CreationTimestamp` for automatic audit timestamps.
- Database constraints must mirror domain invariants (seat counts, contiguous stop ordering).

## OSM Refresh
- OSM data refreshes run monthly through a single-instance production job.
- Imports build in isolated staging, record source and execution provenance, validate the complete generation, and
  reconcile it into durable location records in one transaction.
- Production serves the previous committed catalog until promotion succeeds. Failed staging data is removed after
  its validation report and metadata are retained.
- Promotion rejects a missing or invalid Romania boundary, duplicate durable/source identities, invalid selectable
  names or geometries, broken retained ride references, administrative-unit drops above 10%, street drops above
  15%, unassigned eligible streets above 5%, or parentless non-county units above 2%.
- A failed generation never becomes the new validation baseline and requires explicit operator approval to promote.
