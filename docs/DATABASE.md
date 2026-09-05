# Database & Migrations

PostgreSQL with PostGIS extensions. Schema is versioned using Flyway.

## Migrations
- Location: `src/main/resources/db/migration/`
- Naming format: `V{sequential_integer}__{snake_case_description}.sql` (e.g., `V12__add_ride_indexes.sql`).
- **All migrations are additive.** Never edit, rename, or delete an applied migration.
- Always check migration status before adding a new file: `./mvnw flyway:info`.

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