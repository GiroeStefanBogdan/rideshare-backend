# Database & Migrations

PostgreSQL with PostGIS extensions. Schema is versioned using Flyway.

## Migrations
- Location: `src/main/resources/db/migration/`
- Naming format: `V{sequential_integer}__{snake_case_description}.sql` (e.g., `V12__add_ride_indexes.sql`).
- **All migrations are additive.** Never edit, rename, or delete an applied migration.
- Always check migration status before adding a new file: `./mvnw flyway:info`.
- Flyway exclusively owns permanent tables, columns, extensions, constraints, and indexes. Import jobs may own
  temporary staging structures but must not evolve the permanent schema.

## Migration execution and V6

- `pom.xml` configures Flyway **only as a Maven plugin** (`flyway-maven-plugin` 12.4.0 with
  its PostgreSQL plugin dependency). There is no application Flyway runtime dependency or
  lifecycle-bound migrate execution. Starting Spring Boot or building does not apply migrations.
- Configure the plugin's URL, user, and password for the target database (for example through
  `FLYWAY_URL`, `FLYWAY_USER`, and `FLYWAY_PASSWORD`). Spring datasource configuration is not
  automatically Maven plugin configuration. Then explicitly run:
  ```bash
  ./mvnw flyway:info
  ./mvnw flyway:migrate
  ./mvnw flyway:validate
  ```
- `V6__booking_cancellation_and_required_schedule.sql` is already applied in the development
  database (reported development state, not validation evidence from this documentation update).
  Do not edit it; use a later migration for corrections. Other environments require explicit migrate.
- V6 replaces ride/booking status checks with `ACTIVE`, legacy `INACTIVE`, and `CANCELLED`.
  It does not rewrite legacy statuses; My Rides normalizes them in response mapping.
- V6 adds `ride_stop_schedule_required CHECK (departs_at IS NOT NULL) NOT VALID`.
  New/updated rows must satisfy the check even when legacy null rows remain. If there are no
  legacy nulls, V6 validates the check and sets the column `NOT NULL`; otherwise it leaves the
  check unvalidated and the column nullable. It never fabricates schedules. Any later cleanup
  must use authoritative times before validating/promoting the constraint.

## Cancellation transactions

Passenger cancellation takes pessimistic write locks in **ride → booking → stops** order after
an owner-filtered scalar ride-ID lookup. Reservation also locks the ride before stops. A successful
cancellation marks only its booking `CANCELLED` and adds exactly `booking.seats` to each stop in
`[fromOrder, toOrder)` in the same transaction. No-op retries or a cancelled/inactive ride release
nothing. Driver cancellation updates only the ride status; bookings, stops, and capacity remain.
See [domain invariants](agents/domain.md) for eligibility and [API contract](API.md) for errors.

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
