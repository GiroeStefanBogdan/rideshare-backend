# AGENTS.md — Coding Agent Instructions

# RideShare Backend · Java 25 / Spring Boot 4 · IntelliJ IDEA

This file is the authoritative context document for any AI coding agent working on this repository.
Read it in full before generating, editing, or scaffolding any code in this project.

---

## Project Context

This is the **backend** of a minimalist ride-sharing prototype:

- **Java 25** — virtual threads enabled, modern language features
- **Spring Boot 4.0.6** — REST controllers, Spring Security (stateless JWT), Spring Data JPA
- **PostgreSQL + PostGIS** — OSM-sourced location data
- **SvelteKit frontend** — separate repo, communicates via REST, runs on `http://localhost:5173` by default

For package structure and layer conventions, see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

---

## Commands

### Build & Run

```bash
./mvnw spring-boot:run          # start the dev server (port 8080)
./mvnw clean install            # full build — runs Checkstyle, SpotBugs, and all tests
./mvnw validate                 # Checkstyle only (fast — run this after every edit)
./mvnw compile                  # compile + SpotBugs
./mvnw test                     # all tests
./mvnw test -Dtest=ClassName    # single test class
```

### Database

```bash
./mvnw flyway:migrate           # apply pending migrations
./mvnw flyway:info              # show migration status
```

**After every code change**, run `./mvnw validate` to catch Checkstyle violations before committing.
**Before opening a PR**, the full `./mvnw clean install` must pass.

For database conventions and migration rules, see [`docs/DATABASE.md`](docs/DATABASE.md).

---

## Agent Permissions

### Allowed without asking
- Read any file, run tests, run `./mvnw validate`
- Add or edit source files under `src/main/java/` and `src/test/`
- Create new DB migration scripts in `src/main/resources/db/migration/`

### Ask first
- Adding or changing dependencies in `pom.xml`
- Modifying `SecurityConfig.java`, `JwtFilter.java`, or `CorsConfig.java`
- Changing any existing Flyway migration file (migrations already applied are immutable)
- Changing `application.properties` (especially datasource or JWT config)

### Never
- Commit secrets, credentials, or API keys
- Use `git push` without being explicitly asked
- Modify files in `src/main/resources/db/migration/` that have already been applied to the DB
- Delete or rename existing public API endpoints without being asked (the SvelteKit frontend depends on them)

---

## Non-Negotiable

These rules override everything else. Violating any of them is always wrong, regardless of context.

1. **No useless interfaces.** Do not create an interface for a new Bean unless otherwise asked.
2. **No Lombok.** Write explicit getters, setters, and constructors.
3. **No star imports.** Always use explicit, individual imports. (`AvoidStarImport` is enforced.)
4. **No `var`.** Use explicit types for all variable declarations.
5. **All method parameters must be `final`.** (`FinalParameters` is enforced.)
6. **No Javadoc required.** Add comments only when *why* is not obvious from the code.

For full Checkstyle and SpotBugs rules, see [`docs/CODESTYLE.md`](docs/CODESTYLE.md).

---

## Authentication & Security — Intentional Design Decisions

> These choices may look unusual. They are deliberate — do not "fix" them.

- **Stateless JWT, no sessions** — `SessionCreationPolicy.STATELESS`. There is no server-side session store.
- **HTTP-only cookie for JWT** (not `Authorization` header) — mitigates XSS token theft. The cookie is set and cleared by the backend.
- **CSRF disabled** — safe here because the API is stateless and the JWT cookie is HTTP-only with `SameSite` protection.
- **CORS configured in `CorsConfig.java`** — the SvelteKit frontend at `http://localhost:5173` is the only allowed origin in dev.
- **`EI_EXPOSE_REP` / `EI_EXPOSE_REP2` suppressed globally** — false positives in the Spring/JPA context; do not re-enable them.
- **`SecurityConfig.PUBLIC_ENDPOINTS`** — the source of truth for unauthenticated routes. If you add a public endpoint, register it here.
- Authenticated user is resolved via `@AuthenticationPrincipal UserPrincipal` in controller methods.

---

## Git Workflow

- Branch naming: `feature/<short-description>`, `fix/<short-description>`
- Commit messages: imperative mood, present tense — e.g. `Add ride search endpoint`, not `Added` or `Adding`
- Every commit must pass `./mvnw validate` (Checkstyle)
- Every PR must pass `./mvnw clean install` (full build + tests)
- Do not squash or force-push without being asked

---

## Scaffolding Rules

When asked to add a feature, generate the **complete set of files** for that feature. Do not generate partial stubs.

### Adding a new REST endpoint

Generate together:

1. **DTO** (`dto/<domain>/`) — request/response `record`(s) with validation annotations
2. **Service method** (`service/<domain>/`) — business logic; new service class only if it is a genuinely new domain
3. **Controller method** (`controller/`) — thin handler that delegates to the service
4. **Exception(s)** (`exception/<domain>/`) — if new error cases arise

### Adding a new entity

Generate together:

1. **Entity** (`model/<domain>/`) — JPA entity, `protected` no-arg constructor, explicit getters/setters
2. **Repository** (`repository/<domain>/`) — extends `JpaRepository`, custom queries in `*Impl` if needed
3. **DTO(s)** — never expose the entity directly
4. **SQL migration** (`src/main/resources/db/migration/`) — see [`docs/DATABASE.md`](docs/DATABASE.md) for naming conventions