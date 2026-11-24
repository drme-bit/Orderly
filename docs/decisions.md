# Architecture decisions

A lightweight decision log (ADR-style) for the Orderly backend. Each entry records the
context, the decision and its consequences. New entries are appended; superseded ones
are marked instead of deleted.

Format: **Status** — Accepted / Proposed / Superseded · **Date** — when it was taken.

---

## CSRF protection for cookie-authenticated clients

**Status** — Accepted · **Date** — 2026-11-24

**Context.** Browser clients will authenticate with a cookie session established by the
dedicated auth service / gateway. Cookies are sent automatically on cross-site requests,
so mutating endpoints need proof that the request originated from our frontend. The
auth mechanism itself is not part of `users` yet.

**Decision.** The public `SecurityFilterChain` uses Spring Security 7's `CsrfConfigurer::spa`:
the token is stored in a JS-readable `XSRF-TOKEN` cookie and returned in the
`X-XSRF-TOKEN` header. Failures use the API's `ApiError` JSON shape. User authorization
stays `permitAll()` until the cookie session is wired; at that point it becomes
`authenticated()`.

**Consequences.** All `POST`/`PUT`/`PATCH`/`DELETE` calls without the token now get `403`,
including the existing curl examples and tests. A cross-origin frontend additionally
needs a credentials-allowing CORS configuration. Clients using only a Bearer header do
not need CSRF; this decision is scoped to cookie sessions.

---

## Internal service authentication via a header on a separate prefix

**Status** — Accepted · **Date** — 2026-11-24

**Context.** Internal services need to call `users` with their own identity. A shared
secret in a header is simple to operate, but only safe if it can never be injected by an
external client.

**Decision.** Internal handlers live under `/internal/**`, handled by a separate,
higher-priority `SecurityFilterChain` with `STATELESS` sessions and CSRF disabled. A
filter compares `X-Internal-Token` to `ORDERLY_INTERNAL_TOKEN` with `MessageDigest.isEqual`
and creates a service principal with `SCOPE_internal`. The gateway must strip that header
from client requests and must not expose `/internal/**` publicly. The secret has no
default: startup fails when it is missing.

**Consequences.** Invalid or missing tokens get `401` in the `ApiError` shape. Rotation
requires updating all callers. A per-service mTLS identity would be stronger and is the
natural follow-up if the number of internal callers grows.

---
