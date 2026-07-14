# Code Style

## Checkstyle

`./mvnw validate` runs Checkstyle. Violations fail the build.

- 4-space indentation; no tabs.
- Maximum line length: 120 characters.
- Braces are required; use K&R style.
- All parameters are `final`.
- Use standard modifier order.
- No star imports.
- Keep boolean expressions simplified and switch statements exhaustive with a `default` case.

## Java conventions

- No Lombok, `var`, or useless Bean interfaces.
- Constructor injection; mark fields `final` where possible.
- Explicit getters, setters, and constructors for entities.
- Comments explain non-obvious reasons, not obvious operations.

## SpotBugs

`./mvnw compile` runs SpotBugs. `EI_EXPOSE_REP` and `EI_EXPOSE_REP2` are globally suppressed because of the Spring/JPA model. Do not add global suppressions; use a narrow method-level suppression only when justified.
