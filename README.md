# Orderly

Microservice backend for Orderly. The repository is a monorepo: every service lives in
its own directory and is built, tested and shipped independently.

The first service is [`users`](users) — user profiles and their lifecycle.

## Stack

| Layer            | Choice                                              |
|------------------|-----------------------------------------------------|
| Language         | Java 27                                             |
| Framework        | Spring Boot 4.1 (Web MVC, Data JPA, Security)       |
| Build            | Gradle 9 (wrapper)                                  |
| Database         | PostgreSQL 18                                       |
| Migrations       | Flyway                                              |
| Cache / KV       | Redis 8 (declared, not used by the service yet)     |
| API docs         | springdoc-openapi (`/swagger-ui.html`, `/v3/api-docs`) |
| Local runtime    | Docker Compose                                      |

## Layout

```
.
├── docker-compose.yml        # PostgreSQL + Redis + users service (dev stack)
├── .env.example              # compose variables (copy to .env)
├── docs/
│   ├── api/users.md          # HTTP API reference
│   └── decisions.md          # architecture decisions (ADRs)
└── users/                    # users microservice
    ├── Dockerfile            # multi-stage build, runs as non-root
    ├── build.gradle
    └── src/main
        ├── java/dev/drme/users
        │   ├── api/          # controllers (HTTP only)
        │   ├── application/  # use cases, transactions
        │   ├── config/       # security chain, Clock bean
        │   ├── domain/       # aggregate + value objects, no persistence code
        │   ├── persistence/  # JPA entity, repository, mapper
        │   ├── dto/          # request/response payloads
        │   └── exception/    # ApiError + global handler
        └── resources
            ├── application.yaml
            └── db/migration/ # Flyway migrations
```

## Quick start

Requirements: Docker with the Compose plugin.

```bash
cp .env.example .env        # required: set INTERNAL_SERVICE_TOKEN (see below)
docker compose up -d --build
```

`INTERNAL_SERVICE_TOKEN` is the shared secret for service-to-service calls. Generate it
once with `openssl rand -hex 32` and keep it out of version control. Compose refuses to
start without it.

Then:

| What                | Where                        |
|---------------------|------------------------------|
| Users API           | http://localhost:8080/api/v1/users |
| Swagger UI          | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON        | http://localhost:8080/v3/api-docs |
| PostgreSQL          | `localhost:5432`, db/user `orderly` |
| Redis               | `localhost:6379`             |

```bash
docker compose ps            # all three containers should be healthy
docker compose logs -f users
docker compose down          # keep data
docker compose down -v       # drop data volumes
```

Ports and credentials come from `.env` (`POSTGRES_*`, `REDIS_PORT`, `USERS_PORT`).
The defaults are development-only.

## API

Base path: `/api/v1/users` — full reference in [docs/api/users.md](docs/api/users.md).

| Method | Path                      | Purpose                          |
|--------|---------------------------|----------------------------------|
| GET    | `/api/v1/users`           | list users                       |
| GET    | `/api/v1/users/{id}`      | get one user                     |
| POST   | `/api/v1/users`           | create a user                    |
| PUT    | `/api/v1/users`           | update email / location          |
| DELETE | `/api/v1/users/{id}`      | delete a user                    |
| PUT    | `/api/v1/users/{id}/block`      | ACTIVE/SUSPENDED/INACTIVE → BLOCKED |
| PUT    | `/api/v1/users/{id}/unblock`    | → ACTIVE                         |
| PUT    | `/api/v1/users/{id}/suspend`    | ACTIVE → SUSPENDED               |
| PUT    | `/api/v1/users/{id}/reactivate` | → ACTIVE                         |

User authentication is **not** implemented in this service yet — public endpoints are
open until the dedicated auth service / gateway establishes the cookie session. Mutating
requests are already CSRF-protected (see [the API reference](docs/api/users.md#csrf)).
Internal service-to-service calls use a separate `/internal/**` prefix with a shared
secret header; they are never part of the public ingress.

## Running the service without Docker

```bash
docker compose up -d postgres redis   # infrastructure only
cd users
./gradlew bootRun                     # http://localhost:8080
```

The service reads `localhost:5432` (db/user/password `orderly`) and `localhost:6379`
by default, so no extra configuration is needed for local work.

Build the jar / the image:

```bash
cd users
./gradlew bootJar                     # build/libs/users-0.0.1-SNAPSHOT.jar
docker compose build users            # or: docker build -t orderly/users ./users
```

Tests:

```bash
cd users
./gradlew test                        # needs PostgreSQL on localhost:5432
```

## Configuration

The service is configured through environment variables (Spring relaxed binding):

| Variable                         | Default                                        |
|----------------------------------|------------------------------------------------|
| `SPRING_DATASOURCE_URL`          | `jdbc:postgresql://localhost:5432/orderly`      |
| `SPRING_DATASOURCE_USERNAME`     | `orderly`                                       |
| `SPRING_DATASOURCE_PASSWORD`     | `orderly`                                       |
| `SPRING_JPA_HIBERNATE_DDL_AUTO`  | `validate` (schema is owned by Flyway)         |
| `SPRING_DATA_REDIS_HOST`         | `localhost`                                     |
| `SPRING_DATA_REDIS_PORT`         | `6379`                                          |
| `ORDERLY_INTERNAL_TOKEN`         | none — startup fails if missing                 |

Compose additionally uses `INTERNAL_SERVICE_TOKEN` (mapped to `ORDERLY_INTERNAL_TOKEN`),
`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`, `REDIS_PORT`,
`USERS_PORT`, `TZ` (see `.env.example`).

## Documentation

- [API reference — users](docs/api/users.md)
- [Architecture decisions](docs/decisions.md)

## Known gaps

- User authentication/authorization is deferred to the auth service; public `permitAll()`
  must become `authenticated()` when the cookie session lands.
- Cross-origin frontend access needs a CORS configuration that allows credentials.
- Redis is wired and configured but no code uses it yet.
- No pagination on `GET /api/v1/users`.
- `DELETE /users/{id}` is a hard delete; the `DELETED` status is not reachable via the API.
- REST Docs snippets are wired in the build, but no contract tests exist yet.
