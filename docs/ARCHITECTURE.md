# Architecture

## Layering

```
Controller → Service → Repository
     ↕           ↕           ↕
    DTO        Model      JPA / Native SQL
```

| Layer          | Convention                                                                                            |
|----------------|-------------------------------------------------------------------------------------------------------|
| Controller     | `@RestController`, thin — validates via `@Valid`, delegates to service, returns `ResponseEntity`      |
| Service        | `@Service`, contains all business logic and orchestration                                             |
| Repository     | Extends `JpaRepository`. Custom query logic goes in a `*Impl` class (Spring Data custom repo pattern) |
| DTO            | Java `record`. Validation annotations live on DTO fields, not on entities                             |
| Entity / Model | JPA `@Entity`, explicit getters/setters, `protected` no-arg constructor for JPA                       |
| Exception      | `extends RuntimeException`, annotated with `@ResponseStatus`. Handled by `GlobalExceptionHandler`     |

---

## Package Structure

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

---

## DTOs

- **Always use Java `record`s**
- Place validation annotations (`@NotNull`, `@Size`, `@Min`, `@Positive`, `@Future`, etc.) directly on record components
- Grouped by domain: `dto.auth`, `dto.ride`, `dto.user`, `dto.location`
- Never expose JPA entities directly in API responses

---

## Entities

- Explicit getters and setters — no Lombok
- `protected` no-arg constructor (required by JPA)
- `FetchType.LAZY` for all associations
- `@CreationTimestamp` for `createdAt` fields
- `@Enumerated(EnumType.STRING)` for all enums
- `@JoinColumn` on the owning side, `mappedBy` on the inverse

---

## Exception Handling

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class RideNotFoundException extends RuntimeException {
}
```

- Each domain has its own sub-package: `exception.ride`, `exception.user`
- `GlobalExceptionHandler` (`@RestControllerAdvice`) maps exceptions to `ErrorResponseDto`
- `MethodArgumentNotValidException` is caught globally and returns field-level validation messages
