# Engineering Workflow

## Commands

```bash
./mvnw spring-boot:run
./mvnw validate
./mvnw compile
./mvnw test
./mvnw test -Dtest=ClassName
./mvnw clean install
./mvnw flyway:info
./mvnw flyway:migrate
./mvnw flyway:validate
```

Run `validate` after Java edits. Run `clean install` before a PR.

## Git

- Branches: `feature/<short-description>` or `fix/<short-description>`.
- Commit messages use imperative present tense.
- Do not push, force-push, or squash without instruction.

## Adding an endpoint

Add the complete vertical slice:

1. Request/response DTO records with validation.
2. Service business method or a new service only for a genuinely new domain.
3. Thin controller handler.
4. Domain exceptions and global mapping for new error cases.
5. API documentation and tests.

Register a new unauthenticated route in `SecurityConfig.PUBLIC_ENDPOINTS` only when it is intentionally public.

## Adding an entity

Add the entity, repository, DTOs, and a new sequential Flyway migration. Never edit an applied migration. Check `flyway:info` first.

## Verification

Preserve unrelated worktree changes. Review the diff, run targeted tests, then run `validate`; run the full build for release/PR readiness.
