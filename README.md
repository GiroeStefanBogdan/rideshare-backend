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
./mvnw flyway:info|migrate|validate # Flyway migration tools
```

## Documentation
- [Agent Guide](AGENTS.md) — Architecture, code style, commands & agent boundaries
- [API Contract](docs/API.md) — REST routes, DTOs & wire quirks
- [Database](docs/DATABASE.md) — PostgreSQL, PostGIS & Flyway migrations
- [Security](docs/SECURITY.md) — JWT cookie, CORS & access control
- [Domain Logic](docs/agents/domain.md) — Invariants, pricing & lifecycle