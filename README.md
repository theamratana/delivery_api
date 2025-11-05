# Delivery API

Spring Boot + PostgreSQL delivery API. Secure by default with JWT; runs locally with Docker Postgres; a helper script enforces running on port 8081.

## Quick start

1) Start PostgreSQL via Docker (host 5433 → container 5432):

```bash
docker compose up -d
```

2) Start the API (always on port 8081):

```bash
./run-api.sh start
```

3) Health check:

```bash
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8081/api/ping
```

4) Get a dev token and user (for testing protected endpoints):

```bash
curl -s -X POST http://localhost:8081/auth/dev/token/new
```

Use the returned `token` with `Authorization: Bearer <token>`.

## Prerequisites

- Java 17
- Gradle 8.5 (wrapper unpacked in `./gradle-8.5/`)
- Docker (optional but recommended for Postgres)

## Build and test

```bash
./gradle-8.5/bin/gradle clean build
./gradle-8.5/bin/gradle test
```

## Running locally

Preferred: use the helper script (Windows Git Bash compatible)

- Start: `./run-api.sh start`
- Stop: `./run-api.sh stop` (kills any process bound to :8081)
- Restart: `./run-api.sh restart`
- Status: `./run-api.sh status` (checks if :8081 is in use)

Notes:
- The script enforces `SERVER_PORT=8081` and frees the port before starting.
- Database defaults match the included Docker Compose.

If you prefer raw Gradle:

```bash
DB_URL=jdbc:postgresql://localhost:5433/deliverydb \
DB_USERNAME=postgres \
DB_PASSWORD=postgres \
SERVER_PORT=8081 \
./gradle-8.5/bin/gradle bootRun
```

## Configuration

Environment variables (with defaults):

- DB_URL: `jdbc:postgresql://localhost:5433/deliverydb`
- DB_USERNAME: `postgres`
- DB_PASSWORD: `postgres`
- SERVER_PORT: `8081` (script forces 8081)
- TELEGRAM_BOT_TOKEN: required for Telegram login signature verification
- JWT_SECRET: `dev-secret-change` (plain text or base64; we derive a strong HS256 key)
- JWT_ISSUER: `delivery-api`
- JWT_EXP_MINUTES: `10080` (7 days)
- JWT_DEV_ENABLED: `true` (enables dev token endpoints; disable for prod)

These map to `src/main/resources/application.properties`.

## Security

- Public: `/api/**`, `/auth/**`
- Protected (JWT required): `/users/**`

Obtain a JWT by either:
- Telegram verify: `POST /auth/telegram/verify` (needs `TELEGRAM_BOT_TOKEN`)
- Dev flow (for local testing): `POST /auth/dev/token/new` (creates a placeholder user and returns a token)

Validate requests with:

```
Authorization: Bearer <token>
```

### Test Telegram login with your current session

If you’re already signed in to Telegram on this computer, use the included widget page to get a real signed payload and test the signup flow:

1) Open the file `tools/dev/tg-login.html` in your browser (double-click).
2) Enter your bot username (without @), e.g. `my_delivery_bot`.
3) Click the Telegram button; you’ll see a JSON payload (includes `hash`).
4) Copy the JSON, and in Postman call:
  - POST `http://localhost:8081/auth/telegram/verify`
  - Body: Raw JSON (paste payload)
5) Response includes `{ token, userId, displayName, username, provider }`.

Notes:
- Ensure `TELEGRAM_BOT_TOKEN` is set on the server; the backend verifies the signature with this token.
- If you get `401 invalid_signature`, double-check you used the right bot and token pair.

## API overview

- Health
  - GET `/api/ping` → API liveness
  - GET `/api/db/ping` → DB connectivity

- Auth
  - POST `/auth/telegram/verify` → returns `{ token, userId, displayName, username, provider }`
  - GET `/auth/dev/token/{userId}` → returns `{ token, userId }` for an existing user (dev only). 404 if the userId doesn’t exist.
  - POST `/auth/dev/token/new` → creates a dev user and returns `{ token, userId, username }` (dev only).

- Users (phones; JWT required)
  - GET `/users/{userId}/phones` → list phones
  - POST `/users/{userId}/phones` → add phone
    - Body: `{ "phoneE164": "+1234567890", "primary": true }`
    - Errors: `phoneE164_required`, `phone_already_in_use`
  - PATCH `/users/{userId}/phones/{phoneId}/primary` → set primary phone (enforces one primary)

## Example calls

Create dev token and call a protected endpoint:

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/auth/dev/token/new | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
USER=$(curl -s -X POST http://localhost:8081/auth/dev/token/new | sed -n 's/.*"userId":"\([^"]*\)".*/\1/p')

curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8081/users/$USER/phones
```

Add a phone:

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"phoneE164":"+12025550123","primary":true}' \
  http://localhost:8081/users/$USER/phones
```

## Database

Start Postgres:

```bash
docker compose up -d
```

Inspect tables:

```bash
docker exec -t delivery-postgres psql -U postgres -d deliverydb -Atc "select table_name from information_schema.tables where table_schema='public' order by table_name;"
```

JPA is configured with `spring.jpa.hibernate.ddl-auto=update` for local development.

## Troubleshooting

- 404 on `GET /auth/dev/token/{userId}`: The user doesn’t exist. Use `POST /auth/dev/token/new` first, or login via Telegram.
- 401 Unauthorized: Missing/invalid `Authorization: Bearer <token>` header for protected routes.
- Port 8081 already in use: `./run-api.sh stop` to free it, then `./run-api.sh start`.
- Postgres connection issues:
  - Ensure Docker is running and the container is healthy (`docker ps`, `docker logs delivery-postgres`).
  - DB URL for Docker is `jdbc:postgresql://localhost:5433/deliverydb`.
  - Credentials: `postgres/postgres` (as defined in compose file).

## Notes

- JWT secret can be plain text or base64; we derive a secure HS256 key internally. Use a long, random value in production.
- Dev token endpoints are controlled by `JWT_DEV_ENABLED` and should be disabled in non-dev environments.