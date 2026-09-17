# RideShare Backend

Spring Boot backend for DrumBun ride-sharing (Port 8080).

## Tech Stack
Java 25, Spring Boot 4 (MVC, Security, Data JPA), PostgreSQL/PostGIS, Flyway.

## Commands
```bash
./mvnw spring-boot:run              # Run server
./mvnw validate                     # Run Checkstyle
./mvnw test                         # Run unit/integration tests
./mvnw clean install                # Full build
./mvnw flyway:info                  # Inspect target migration state
./mvnw flyway:migrate               # Explicitly apply migrations
./mvnw flyway:validate              # Validate migration history
```

Flyway is configured only as a Maven plugin, not an application runtime dependency. Configure
its target database credentials separately and run `flyway:migrate` explicitly before starting
against an unmigrated environment. V6 is already applied in dev (reported state); see
[Database](docs/DATABASE.md#migration-execution-and-v6) for required schedules and legacy-null handling.

## My Rides & passenger cancellation

- `GET /rides/me` keeps four arrays, classified by booking drop-off / hosted final-stop time.
  Past includes both `now.minusMonths(1)` and `now`; null ends remain Upcoming.
- Wire status is `ACTIVE` / `CANCELLED`, composing booking and ride status and normalizing legacy
  `INACTIVE`. Driver cancellation changes only the ride status.
- `DELETE /rides/me/bookings/{bookingId}` cancels the passenger's own booking strictly before pickup,
  restoring only its reserved segment seats once. Already cancelled/inactive booking or ride is a no-op.
- Frontend `/my-rides` shows Upcoming + Cancelled; `/my-rides/past` shows active history only.
- See the [API contract](docs/API.md#passenger-cancellation) for owner-only 404,
  idempotent 204, and expired cancellation 409 responses.

## Documentation
- [Agent Guide](AGENTS.md) — Architecture, code style, commands & agent boundaries
- [API Contract](docs/API.md) — REST routes, DTOs & wire quirks
- [Database](docs/DATABASE.md) — PostgreSQL, PostGIS & Flyway migrations
- [Security](docs/SECURITY.md) — JWT cookie, CORS & access control
- [Domain invariants](docs/agents/domain.md) and [glossary](CONTEXT.md)
- [Cancellation ADR](docs/adr/0003-use-explicit-cancelled-status.md) — Explicit status and legacy compatibility
- [Domain Logic](docs/agents/domain.md) — Invariants, pricing & lifecycle