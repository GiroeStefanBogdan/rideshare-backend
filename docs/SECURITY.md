# Security

## Authentication

- Authentication is stateless JWT.
- The token is read from the HTTP-only `token` cookie.
- Clients must send cookies; do not send an `Authorization: Bearer` header.
- Authenticated controllers resolve the user with `@AuthenticationPrincipal UserPrincipal`.
- Session creation policy is `STATELESS`.

## Route access

`/login`, `/register`, `/error`, and `/dashboard` are public. CORS preflight (`OPTIONS /**`) is public. All other routes require authentication unless `SecurityConfig.PUBLIC_ENDPOINTS` changes.

Admin operations use method security with `hasRole('ADMIN')`.

## Cookie and CORS behavior

Login sets an HTTP-only cookie named `token`, with `SameSite=Lax`, path `/`, and a development `secure=false` setting. CORS allows `http://localhost:5173` and `http://127.0.0.1:5173` with credentials.

The frontend must use the shared API client with `credentials: 'include'`.

## CSRF

CSRF is intentionally disabled for the current stateless API design. Do not change this, cookie policy, or CORS without explicit approval.

## Configuration safety

JWT and datasource configuration must not contain committed production secrets. Runtime secrets belong in environment/configuration management. Do not copy secret values into documentation or tests.

## Known gaps

- There is no backend logout endpoint; clearing an HTTP-only cookie requires a server response.
- Login returns the JWT in its JSON body as well as setting the cookie, despite the cookie-only client design.
- Development configuration contains a static JWT secret and should be externalized and rotated in a separate security task.
