# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`AGENTS.md`** at the repo root — the authoritative context document (project context, conventions, non-negotiables, scaffolding rules).
- **`docs/ARCHITECTURE.md`** — layering, package structure, DTO/entity/exception conventions.
- **`docs/CODESTYLE.md`** — Checkstyle rules, SpotBugs settings, general style rules.
- **`docs/DATABASE.md`** — PostgreSQL, PostGIS, Flyway migration conventions.

These files together serve the role of `CONTEXT.md` + `docs/adr/` — they define the domain language, architectural decisions, and coding conventions.

## Use the conventions

When your output references a domain concept, layer boundary, or coding rule, use the terms as defined in `AGENTS.md` and the `docs/` files. Don't drift to synonyms or invent alternative names.

If a concept you need isn't covered by existing documentation, note it as a gap — `/grill-with-docs` can resolve it later.
