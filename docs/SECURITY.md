# Security & Access Control

## Authentication & Sessions
- **Stateless JWT**: Stored in HTTP-only cookie named `token` (`SameSite=Lax`, `path=/`, `secure=false` in dev).
- **Client auth**: Must use `credentials: 'include'`. Do **not** send `Authorization: Bearer` headers.
- **Controller injection**: Resolve authenticated user via `@AuthenticationPrincipal UserPrincipal`.
- **Session policy**: `STATELESS`. CSRF protection is intentionally disabled.

## Route Protection
- **Public**:
    - Auth/Errors: `/login`, `/register`, `/auth/logout`, `/error`, `/dashboard`
    - Locations/Rides: `/locations/search`, `/rides/search`, `GET /rides/{id}`, `GET /users/{id}`
    - Preflight: `OPTIONS /**`
- **Admin**: Require `ROLE_ADMIN` using `@PreAuthorize("hasRole('ADMIN')")`.
- **Protected**: All other routes require an authenticated JWT cookie.

## CORS Configuration
- Allowed origins: `http://localhost:5173`, `http://127.0.0.1:5173`.
- Allow credentials must be `true`.

## Known Gaps & Rules
- `POST /login` returns the JWT in the JSON body as well as setting the cookie; clients must rely on the cookie.
- Dev JWT secret is static and must be externalized for production.
- Never commit credentials, private keys, or tokens into code, tests, or documentation.