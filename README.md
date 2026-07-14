# RideShare Backend

Spring Boot backend for the DrumBun ride-sharing prototype.

## Stack

Java 25, Spring Boot 4, Spring MVC, Spring Security, Spring Data JPA, PostgreSQL/PostGIS, and Flyway. Exact dependency versions are defined in `pom.xml`.

## Quick start

```bash
./mvnw spring-boot:run
./mvnw validate
./mvnw test
./mvnw clean install
```

The server runs on port 8080. PostgreSQL settings come from environment variables referenced by `src/main/resources/application.yml`.

## Documentation

- [API](docs/API.md) — authoritative REST contract
- [Architecture](docs/ARCHITECTURE.md) — layers and package responsibilities
- [Security](docs/SECURITY.md) — JWT cookie, CORS, and route protection
- [Database](docs/DATABASE.md) — PostgreSQL, PostGIS, and Flyway
- [Code style](docs/CODESTYLE.md) — Checkstyle and SpotBugs rules
- [Workflow](docs/WORKFLOW.md) — commands, Git, testing, and scaffolding
- [Domain guide](docs/agents/domain.md) — business vocabulary and invariants
