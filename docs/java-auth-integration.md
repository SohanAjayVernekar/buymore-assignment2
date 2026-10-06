# Java authentication API

Implemented in `backend/` with Java 21 and Spring Boot. Vite and Nginx use the same-origin `/api` proxy.

## Browser flow

Before each mutating request, the frontend fetches `GET /api/auth/csrf` with cookies. It returns `{token, headerName}`; send that header and token on the mutation. Spring stores the CSRF secret in the session and returns a masked token. Login rotates the session ID and clears the old CSRF token. Logout invalidates the session. The API client fetches a fresh token as needed, including after login/logout.

- `POST /api/auth/register`: `{name, email, password, role, invitationCode?}`. Returns 201 with `{ok: true}`. Does not automatically sign in. Passwords require 8–72 characters and at most 72 UTF-8 bytes.
- `POST /api/auth/login`: `{email, password, role}`. Returns `{user: {id, name, email, role}}` and establishes an HttpOnly session cookie.
- `GET /api/auth/me`: returns `{id, name, email, role}`; 401 if unauthenticated.
- `POST /api/auth/logout`: invalidates the session; returns `{ok: true}`.
- Errors use `{message}` with an appropriate status. Validation: 400; unauthenticated: 401; unauthorized/CSRF/invitation: 403; duplicate: 409.

Roles are `CUSTOMER` and `ADMIN`. Admin registration requires a matching server-side `ADMIN_INVITATION_CODE`; a blank configured code disables it. The local `.env` holds the private invitation, never frontend code or version control. It is reusable; rotate it after onboarding.

## Authorization

Product browsing is public. Inactive catalog access, product edits, order search/reindex, and status changes require ADMIN. Checkout uses the authenticated user ID, ignoring any submitted user ID. A customer may fetch only their own order. The frontend hides admin links but server checks are authoritative.

Session expiry is 30 minutes. Password hashes are BCrypt with cost 12. Existing users without password hashes cannot authenticate. No default accounts/passwords, email verification, password recovery or brute-force rate limiter are included.

For HTTPS-only deployment set `COOKIE_SECURE=true`. Use deployment-specific database credentials and keep the application behind HTTPS. Current Compose ports are local only.

## Tests

`mvn -f backend/pom.xml -Pintegration verify` exercises real backing services with isolated test databases, a separate Elasticsearch index, and a separate RabbitMQ queue. Tests cover auth/roles, invitation rejection, duplicates, password hashing, logout, CSRF/session rotation, product filtering, pagination validation, checkout totals, ownership, and queued indexing.
