# Agent Guide

Entry point for LLM agents. Frontend repository: `/home/adicu/rideshare-frontend`.

## Task Routing
Read specialized documentation only when working in that area:
- **Endpoints / API Contract**: `docs/API.md`
- **Database / PostGIS / Migrations**: `docs/DATABASE.md`
- **Auth / Cookies / CORS**: `docs/SECURITY.md`
- **Domain Logic / Invariants**: `docs/agents/domain.md`

## Common Commands
- Run app: `./mvnw spring-boot:run` (port 8080)
- Lint check: `./mvnw validate` (Checkstyle — run after any Java edit)
- Compile & SpotBugs: `./mvnw compile`
- Run tests: `./mvnw test` or targeted `./mvnw test -Dtest=ClassName`
- Full build: `./mvnw clean install` (run before finalizing tasks)

## Architecture & Scaffolding
`Controller (HTTP/DTO)` -> `Service (Business/Tx/Mapping)` -> `Repository (Data)` -> `Database`
- **Adding an endpoint (vertical slice)**:
    1. Request/response Java records in `dto/<domain>` with Bean Validation on request fields.
    2. Business logic, transactions (`@Transactional`), and entity-to-DTO mapping in `service/<domain>`.
    3. Thin controller handler in `controller/`.
    4. Domain exceptions in `exception/<domain>`, handled by global exception handler.
    5. If public, register in `SecurityConfig.PUBLIC_ENDPOINTS`. Update `docs/API.md` and add tests.

## Non-Negotiable Code Style
- **No Lombok, no `var`, no star imports.**
- **All method parameters must be `final`.**
- **Explicit getters, setters, and constructors** on JPA entities (protected no-arg constructor).
- **No Bean interfaces** for services unless multiple implementations exist.
- **Never expose JPA entities directly via APIs.** Always map to DTO records.
- **Date formats**: ISO-8601 for `OffsetDateTime`, `yyyy-MM-dd` for `LocalDate`.
- **Formatting**: 4 spaces, no tabs, max 120 chars/line, K&R braces required, `switch` requires `default`.
- **SpotBugs**: `EI_EXPOSE_REP` and `EI_EXPOSE_REP2` are globally suppressed for Spring/JPA; do not add new global suppressions.

## Git Conventions
- Branches: `feature/<name>` or `fix/<name>`.
- Commit messages: Imperative present tense (e.g., `Add search endpoint filter`).
- Never push, force-push, or squash without instruction.

## Boundaries
- **Autonomous**: Read files, execute tests/checks, write code/tests, create additive migrations.
- **Ask First**: Modifying `pom.xml`, `SecurityConfig`, `JwtFilter`, `CorsConfig`, `application*.yml`, or destructive DB changes.
- **Forbidden**: Modifying applied Flyway migrations; pushing/force-pushing Git branches.

## Sources of Truth
- Dependencies: `pom.xml` | Runtime config: `src/main/resources/application*.yml`
- API contract: `docs/API.md` | Security: `docs/SECURITY.md` | DB: `docs/DATABASE.md`