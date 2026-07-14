# Architecture

## Dependency direction

```text
Controller -> Service -> Repository -> Database
     |           |           |
    DTO        Model      JPA/native SQL
```

Controllers handle HTTP concerns and validation. Services own business rules and orchestration. Repositories own persistence. DTOs are the only API boundary types.

## Packages

- `config`: security, JWT filtering, CORS, and web configuration.
- `controller`: thin REST handlers.
- `dto`: request and response records grouped by domain.
- `exception`: domain exceptions and the global REST exception handler.
- `model`: JPA entities and enums grouped by domain.
- `repository`: Spring Data repositories; custom persistence logic belongs in `*Impl` classes.
- `service`: domain services and authentication services.

Domains are `auth`, `location`, `ride`, and `user`; user-car code is nested under `user`.

## DTO rules

- Use records for new request and response DTOs.
- Put validation annotations on request DTO components, not entities.
- Never serialize entities directly.
- Keep request and response shapes separate. Existing `LoginRequest` is a legacy class exception.
- Use ISO-8601 values for `OffsetDateTime` and `yyyy-MM-dd` for `LocalDate`.

## Entity rules

- JPA entities have explicit accessors and a protected no-argument constructor.
- Associations are lazy unless a query explicitly fetches what a response needs.
- Use `@Enumerated(EnumType.STRING)` for new enum fields.
- Use `@CreationTimestamp` for generated creation timestamps.
- The owning side declares `@JoinColumn`; inverse collections use `mappedBy`.

## Persistence rules

- Derived Spring Data queries belong on repository interfaces.
- Complex or native queries belong in a repository `*Impl` class.
- Transactions belong at service operations that coordinate multiple writes or locking.
- Map entities to DTOs inside the service layer.

## Exceptions

Domain exceptions live under `exception/<domain>`. The global handler maps known exceptions to `ErrorResponseDto`; new error cases must define their HTTP behavior.
