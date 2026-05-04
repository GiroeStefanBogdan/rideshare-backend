# Code Style Reference

## Checkstyle

Enforced at the `validate` phase — **the build fails on any violation.**
Run `./mvnw validate` after every edit to catch issues early.

| Rule                | Value                                                              |
|---------------------|--------------------------------------------------------------------|
| Max line length     | **120** characters                                                 |
| Indentation         | **4 spaces**, no tabs (`FileTabCharacter`)                         |
| Braces              | Always required (`NeedBraces`), K&R style (`LeftCurly`)            |
| Parameters          | All `final` (`FinalParameters`)                                    |
| Modifiers           | Standard order (`ModifierOrder`), no redundant modifiers           |
| Boolean expressions | Simplified (`SimplifyBooleanExpression`, `SimplifyBooleanReturn`)  |
| Switch              | Must have `default` case                                           |
| Imports             | No star imports (`AvoidStarImport`) — always use explicit imports  |

---

## SpotBugs

Enforced at the `compile` phase — **the build fails on any violation.**

**Globally suppressed patterns:**

| Pattern          | Reason                                              |
|------------------|-----------------------------------------------------|
| `EI_EXPOSE_REP`  | False positive in Spring/JPA context — do not remove |
| `EI_EXPOSE_REP2` | False positive in Spring/JPA context — do not remove |

Do not add new global suppressions without discussion. Prefer `@SuppressFBWarnings` at the method level if a specific suppression is needed.

---

## General Style Rules

- Mark fields `final` where possible; all parameters `final` always
- Constructor injection with `@Autowired` on the constructor — not field injection
- Self-documenting names over comments that restate the code
- No Javadoc required — comment only when *why* is non-obvious
