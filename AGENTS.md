# AGENTS.md — Coding Agent Instructions

# RideShare Backend · Java 25 / Spring Boot 4 · IntelliJ IDEA

This file is the authoritative context document for any AI coding agent working on this repository.
Read it in full before generating, editing, or scaffolding any code in this project.

---

## Project Context

This is the **backend** of a minimalist ride-sharing prototype:

- **Java 25** — virtual threads enabled, modern language features
- **Spring Boot 4.0.6** — REST controllers, Spring Security (stateless JWT), Spring Data JPA
- **PostgreSQL + PostGIS** — spatial data via native SQL (hibernate-spatial is **not** a dependency), OSM-sourced location data
- **SvelteKit frontend** — separate repo, communicates via REST, runs on `http://localhost:5173` by default

Core features: email login, posting rides with multi-stop routes, searching rides by location (PostGIS spatial filtering), user profiles, reviews, user cars.
There is **no GPS, no maps, and no real-time tracking** at this stage.

---

## Non-Negotiables

These rules override everything else. Violating any of them is always wrong, regardless of context.

1. **No useless interfaces.** Do not create an interface for a service, repository custom implementation, or any other class unless there is a concrete need for multiple implementations or a framework requires it (e.g., Spring Data `JpaRepository`). A single-implementation interface adds complexity for zero benefit. If only one class will ever implement an interface, write the class directly.
2. **No Lombok.** This project does not use Lombok. Never add `@Getter`, `@Setter`, `@Data`, `@Builder`, or any Lombok annotation. Write explicit getters, setters, and constructors.
3. **No star imports.** Checkstyle enforces `AvoidStarImport`. Always use explicit, individual imports.
4. **No `var`.** Use explicit types for all variable declarations. Clarity over brevity.
5. **All method parameters must be `final`.** Checkstyle enforces `FinalParameters`.
6. **No tests in production code.** Test code belongs exclusively under `src/test/`. Never mix test utilities into `src/main/`.
7. **No production builds or CI/CD.** Never suggest `mvn package`, Docker, or deployment steps unless explicitly asked.
8. **No Javadoc is required.** Checkstyle has Javadoc checks set to `ignore`. Do not generate boilerplate Javadoc. Add comments only when the *why* is not obvious.

---

## Architecture & Layering

```
Controller → Service → Repository
     ↕           ↕           ↕
    DTO        Model      JPA / Native SQL
```

### Package structure

```
com.example.blablacar/
├── RideshareBackendApplication.java
├── config/                 # Security, CORS, Web config, JWT filter
│   ├── CorsConfig.java
│   ├── JwtFilter.java
│   ├── SecurityConfig.java
│   └── WebConfig.java
├── controller/             # REST controllers — thin, delegate to services
│   ├── DashboardController.java
│   ├── LocationController.java
│   ├── RideController.java
│   ├── UserCarController.java
│   └── UserController.java
├── dto/                    # Data Transfer Objects — Java records, grouped by domain
│   ├── auth/               # LoginRequest, LoginResponse, ErrorResponseDto
│   ├── location/           # LocationResultDTO
│   ├── ride/               # RideDTO, RideDriverDTO, RideSearchRequestDTO, RideSearchResultDTO,
│   │                       #   RideStopBasicDTO, RideStopDTO
│   └── user/               # UpdateUserRequest, UserProfileDto, UserPublicProfileDto,
│       │                   #   UserRegistrationRequestDto, UserResponseDto
│       └── car/            # UpdateUserCarRequest, UserCarRequest, UserCarResponse
├── exception/              # Custom exceptions + GlobalExceptionHandler
│   ├── GlobalExceptionHandler.java
│   ├── ride/               # ForbiddenRideException, InvalidRideStopException,
│   │                       #   RideDateTooDistantException, RideNotFoundException
│   └── user/               # EmailAlreadyExistsException, InvalidAgeException,
│                           #   UserCarNotFoundException, UserNotFoundException
├── model/                  # JPA entities — grouped by domain
│   ├── enums/              # AuthProvider, Gender, Role, Status
│   ├── location/           # AdministrativeUnit, AdministrativeUnitType, Street
│   ├── ride/               # Ride, RideStop
│   └── user/               # User, UserCar, UserInfo, UserPrincipal, UserReview
├── repository/             # Spring Data repositories — grouped by domain
│   ├── location/           # AdministrativeUnitRepository, StreetRepository
│   ├── ride/               # RideRepository, RideSearchRepository, RideSearchRepositoryImpl,
│   │                       #   RideStopRepository
│   └── user/               # UserRepository
│       └── car/            # UserCarRepository
└── service/                # Business logic — grouped by domain
    ├── auth/               # JWTService
    ├── location/           # LocationService
    ├── ride/               # RideService
    └── user/               # CustomUserDetailsService, UserService
        └── car/            # UserCarService
```

### Key conventions

| Layer          | Convention                                                                                    |
|----------------|-----------------------------------------------------------------------------------------------|
| Controller     | `@RestController`, thin — validates via `@Valid`, delegates to service, returns `ResponseEntity` |
| Service        | `@Service`, concrete class (no interface). Contains all business logic and orchestration        |
| Repository     | Extends `JpaRepository`. Custom query logic goes in a `*Impl` class (Spring Data custom repo pattern) |
| DTO            | Java `record`. Validation annotations live on DTO fields, not on entities                      |
| Entity / Model | JPA `@Entity`, explicit getters/setters, `protected` no-arg constructor for JPA                |
| Exception      | `extends RuntimeException`, annotated with `@ResponseStatus`. Handled by `GlobalExceptionHandler` |

---

## Code Style & Formatting

### Checkstyle (enforced at `validate` phase — build fails on violations)

| Rule                 | Value                                                    |
|----------------------|----------------------------------------------------------|
| Max line length      | **120** characters                                       |
| Indentation          | **4 spaces**, no tabs (enforced by `FileTabCharacter`)   |
| Imports              | No star imports, no unused imports, no redundant imports  |
| Braces               | Always required (`NeedBraces`), K&R style (`LeftCurly`)  |
| Parameters           | All `final` (`FinalParameters`)                          |
| Naming               | `camelCase` for methods/variables, `PascalCase` for types, `UPPER_SNAKE` for constants (except `log`/`logger`) |
| Modifiers             | Standard order (`ModifierOrder`), no redundant modifiers  |
| Boolean expressions  | Simplified (`SimplifyBooleanExpression`, `SimplifyBooleanReturn`) |
| Switch               | Must have `default` case                                 |
| Utility classes       | Must hide constructor (`HideUtilityClassConstructor`)    |

### SpotBugs (enforced at `compile` phase)

Runs on every build. Globally suppressed: `EI_EXPOSE_REP` and `EI_EXPOSE_REP2` (false positives in Spring/JPA context).

### General style rules

- `const`-correctness: mark all parameters `final`, mark fields `final` when possible
- Prefer constructor injection with `@Autowired` on the constructor
- One class per file, always
- Only comment the **why**, never the **what**
- Prefer self-documenting names over comments that restate the code

---

## DTOs

- **Always use Java `record`s** for DTOs
- Place validation annotations (`@NotNull`, `@Size`, `@Min`, `@Positive`, `@Future`, etc.) directly on record components
- DTOs are grouped by domain: `dto.auth`, `dto.ride`, `dto.user`, `dto.location`
- Never expose JPA entities directly in API responses — always map to a DTO

---

## Entities

- JPA entities use explicit getters and setters (no Lombok)
- Entities must have a `protected` no-arg constructor for JPA
- Use `FetchType.LAZY` for all associations by default
- Use `@CreationTimestamp` for `createdAt` fields
- Use `@Enumerated(EnumType.STRING)` for enums (not `ORDINAL`, unless there is an explicit reason)
- Relationships use the owning-side pattern (`@JoinColumn` on the owning side, `mappedBy` on the inverse)

---

## Exception Handling

Exceptions follow this pattern:

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class RideNotFoundException extends RuntimeException {
}
```

- Each domain has its own exception sub-package: `exception.ride`, `exception.user`
- `GlobalExceptionHandler` (`@RestControllerAdvice`) maps exceptions to structured JSON responses using `ErrorResponseDto`
- Validation errors from `@Valid` are caught as `MethodArgumentNotValidException` and returned as field-level messages

---

## Authentication & Security

- **Stateless JWT** — no sessions, `SessionCreationPolicy.STATELESS`
- JWT is carried in an **HTTP-only cookie** (set and cleared by the backend)
- `JwtFilter` extracts the cookie, validates the token, and sets the `SecurityContext`
- Authenticated user is resolved via `@AuthenticationPrincipal UserPrincipal` in controller methods
- Public endpoints are listed in `SecurityConfig.PUBLIC_ENDPOINTS`
- CSRF is disabled (stateless API)
- CORS is configured in `CorsConfig.java`

---

## Database & Persistence

- **PostgreSQL** with **PostGIS** extension (spatial queries via native SQL — hibernate-spatial is **not** a dependency)
- Connection pool: **HikariCP** (15 connections, prepared statement caching enabled)
- `open-in-view: false` — no lazy loading outside of transactions
- Batch inserts/updates enabled (`jdbc.batch_size: 25`)
- Virtual threads enabled (`spring.threads.virtual.enabled: true`)
- Database migrations live under `src/main/resources/db/migration/` (numbered V2__ pattern, no V1 baseline)
- Location data (administrative units, streets) is pre-loaded from OpenStreetMap via scripts in `osm refresh/`

### Profiles

| Profile   | Purpose                                                   |
|-----------|-----------------------------------------------------------|
| `dev`     | Default active profile, development database              |
| `local`   | Local development with specific overrides                 |

---

## Spatial / Location Model

The location model uses OpenStreetMap-sourced data stored in PostGIS-enabled tables:

- **`AdministrativeUnit`** — cities, counties, regions with `Geometry` columns
- **`Street`** — streets within administrative units, with `Geometry` columns  
- **`AdministrativeUnitType`** — enum distinguishing `STREET` vs administrative unit types
- Ride search uses `ST_Intersects` / `ST_Distance` via native SQL in `RideSearchRepositoryImpl`

---

## Scaffolding Rules

When asked to add a feature, generate the **complete set of files** for that feature.

### Adding a new REST endpoint

Generate together:

1. **DTO** (`dto/<domain>/`) — request/response `record`(s) with validation annotations
2. **Service method** (`service/<domain>/`) — business logic in the existing service, or a new service class if a new domain
3. **Controller method** (`controller/`) — thin handler that delegates to the service
4. **Exception(s)** (`exception/<domain>/`) — if new error cases arise

### Adding a new entity

Generate together:

1. **Entity** (`model/<domain>/`) — JPA entity, `protected` no-arg constructor, explicit getters/setters
2. **Repository** (`repository/<domain>/`) — extends `JpaRepository`, custom queries if needed
3. **DTO(s)** — never expose the entity directly
4. **SQL migration** (`src/main/resources/db/migration/`) — if schema changes are needed

### Never generate (unless explicitly instructed)

- Interfaces for single-implementation services
- Lombok annotations
- Docker / Docker Compose files
- CI/CD pipeline configuration
- OpenAPI / Swagger annotations (unless asked)
- Boilerplate Javadoc

---

## Build & Run

Checkstyle (`maven-checkstyle-plugin:3.6.0`) runs at the `validate` phase and SpotBugs (`spotbugs-maven-plugin:4.9.8.3`) runs at `compile`. Both fail the build on violations. Configuration files:

- `checkstyle.xml` — Checkstyle rules
- `checkstyle-suppressions.xml` — Checkstyle suppressions
- `spotbugs-exclude.xml` — SpotBugs exclusion filters (suppresses `EI_EXPOSE_REP`/`EI_EXPOSE_REP2`)

```bash
# Run the application (dev profile)
./mvnw spring-boot:run

# Compile (triggers Checkstyle + SpotBugs)
./mvnw compile

# Run tests
./mvnw test
```

The app runs on `http://localhost:8080` by default.

---

## Comments

- Only comment the **why**, never the **what**
- Prefer self-documenting names over comments
- Author/date headers are acceptable but not required
- A `//TODO` is fine for tracking future work
