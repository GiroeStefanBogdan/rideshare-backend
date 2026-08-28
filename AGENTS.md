# RideShare Backend Agent Guide

Authoritative entry point for agents working in this repository.

## Stack

- Java 25; exact Spring Boot and dependency versions come from `pom.xml`.
- Spring MVC, Spring Security, Spring Data JPA, PostgreSQL/PostGIS, and Flyway.
- SvelteKit frontend is a separate repository at `/home/adicu/rideshare-frontend`.

## Read by task

| Task | Read |
| --- | --- |
| Any code change | `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/CODESTYLE.md` |
| REST endpoint or frontend integration | `docs/API.md`, `docs/SECURITY.md`, `docs/agents/domain.md` |
| Entity, query, or migration | `docs/DATABASE.md`, `docs/agents/domain.md` |
| Build, tests, Git, or scaffolding | `docs/WORKFLOW.md` |
| Security, cookies, CORS, or public routes | `docs/SECURITY.md` |

`CONTEXT.md` is a compatibility pointer to this file.

## Non-negotiable rules

- Do not create useless Bean interfaces.
- Do not use Lombok, star imports, or `var`.
- All method parameters are `final`.
- Use explicit getters, setters, and constructors.
- Do not expose JPA entities directly from APIs.
- Do not change public endpoints, security configuration, or applied migrations without explicit approval.
- Never commit credentials, tokens, or API keys.

## Permission boundaries

Allowed: read files, run tests/checks, edit Java/tests, and add migrations.

Ask first: dependency changes, `SecurityConfig.java`, `JwtFilter.java`, `CorsConfig.java`, application configuration, or destructive schema changes.

Never modify an applied Flyway migration. Never push or force-push without explicit instruction.

## Source of truth

- Build/dependencies: `pom.xml`
- Runtime configuration: `src/main/resources/application*.yml`
- API behavior: `docs/API.md` and controller/DTO code
- Security behavior: `docs/SECURITY.md` and security config code
- Database behavior: migrations and `docs/DATABASE.md`
- Business vocabulary: `docs/agents/domain.md`
