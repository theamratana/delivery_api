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
- TELEGRAM_BOT_TOKEN: required for Telegram login signature verification and OTP bot messaging
- TELEGRAM_BOT_USERNAME: patience_delivery_bot (default) — your bot's username without @
- TELEGRAM_POLLING_ENABLED: true (dev only; enables polling for Telegram updates)
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

**Audit Trail**: User profile updates from Telegram login are logged in the `user_audits` table, tracking changes to `displayName`, `firstName`, `lastName`, `username`, `avatarUrl`.

### Telegram OTP (phone verification)

This flow lets a user enter a phone number on your site, then receive a 6‑digit OTP via your Telegram bot after verifying their phone ownership. The OTP verifies that the user has access to the entered phone number.

Requirements:
- Set `TELEGRAM_BOT_TOKEN` (from BotFather).
- Ensure your bot username is set (default `patience_delivery_bot` or set `TELEGRAM_BOT_USERNAME`).
- For local/dev, leave `TELEGRAM_POLLING_ENABLED=true` so the server polls Telegram for updates.

Flow:
1) Create an OTP attempt

```bash
curl -s -X POST http://localhost:8081/auth/otp/request \
  -H 'Content-Type: application/json' \
  -d '{"phone_e164":"+12025550123"}'
```

Response (new user, requires phone verification):

```json
{
  "attemptId": "<uuid>",
  "deepLink": "https://t.me/<your_bot_username>?start=link_<code>",
  "expiresAt": "2025-01-01T12:00:00Z",
  "sentDirectly": false
}
```

Response (existing user, OTP sent directly):

```json
{
  "attemptId": "<uuid>",
  "deepLink": null,
  "expiresAt": "2025-01-01T12:00:00Z",
  "sentDirectly": true
}
```

2) For new users: Have the user tap the `deepLink` in Telegram. This will open your bot and send `/start link_<code>` to the backend. The bot will ask the user to share their phone number. If the shared phone matches the entered phone, the bot sends a 6‑digit OTP.

For existing users: The OTP is sent directly to their linked Telegram chat.

3) Verify the OTP

```bash
curl -s -X POST http://localhost:8081/auth/otp/verify \
  -H 'Content-Type: application/json' \
  -d '{"attemptId":"<uuid>","code":"<6digits>"}'
```

Response:

```json
{ "token": "<jwt>", "userId": "<uuid>" }
```

Notes:
- The bot can only message users who have started the bot at least once; the deep link ensures this is satisfied.
- For new users, phone verification is done by requiring the user to share their contact in Telegram, ensuring the phone matches.
- OTP attempts expire in a few minutes; verification will fail if expired or too many attempts are made.
- In dev, if `TELEGRAM_BOT_TOKEN` is not set, OTP messages cannot be sent. Set the token or disable polling with `TELEGRAM_POLLING_ENABLED=false`.

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
  - POST `/auth/otp/request` → body `{ "phone_e164": "+12025550123" }` returns `{ attemptId, deepLink, expiresAt }`
  - POST `/auth/otp/verify` → body `{ "attemptId": "<uuid>", "code": "123456" }` returns `{ token, userId }`

- Deliveries
  - POST `/deliveries/summary` → returns counts grouped by delivery status for the authenticated user.
    - Body example (date range in ISO 8601):
      ```json
      { "startDate": "2025-11-01", "endDate": "2025-11-29" }
      ```
    - Response example:
      ```json
      {
        "CREATED": 10,
        "ASSIGNED": 3,
        "PICKED_UP": 2,
        "IN_TRANSIT": 1,
        "OUT_FOR_DELIVERY": 0,
        "DELIVERED": 20,
        "CANCELLED": 0,
        "RETURNED": 0,
        "FAILED": 0
      }
      ```

  - PATCH `/deliveries/{id}/status` → change a delivery item's status (authenticated + authorized)
    - Body example:
      ```json
      { "status": "DELIVERED", "note": "Delivered to front door" }
      ```
    - Notes: Allowed for sender, receiver, delivery driver, or company OWNER/MANAGER/system admin. Creates a delivery tracking entry when status is changed.

  - Note storage: When a status change occurs the message/note provided in the request is saved in the `delivery_tracking.description` field (history) and also in `delivery_items.last_status_note` (quick access on the delivery row). A migration file `migration-add-last-status-note.sql` is included to add this column.

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

### Reset Database (Clear All Data)

To clear all data and start fresh for testing:

**Windows:**
```cmd
reset-db.bat
```
*Script will prompt for credentials if not set, and ask for confirmation*

**Linux/Mac:**
```bash
./reset-db.sh
```
*Script will prompt for credentials if not set, and ask for confirmation*

**Manual SQL:**
```bash
psql -U postgres -d deliverydb -f reset-db.sql
```

The scripts will:
- ✅ Use existing `DB_USERNAME`/`DB_PASSWORD` environment variables if set
- ✅ Prompt for credentials if not set (with sensible defaults)
- ✅ Ask for confirmation before deleting data
- ✅ Show verification counts after reset

### Targeted Database Reset (Preserve Working Data)

For development troubleshooting, reset only specific modules while preserving working features:

**✅ INTERACTIVE MENU SYSTEM:** Choose modules with numbered options instead of editing code!

**How to Use:**
1. Run `./reset-db.sh` (Linux/Mac) or `reset-db.bat` (Windows)
2. Select module numbers (e.g., "1 3 5" for multiple modules)
3. Option 0 for full reset (CAUTION!)
4. Confirm and the script generates/executes the appropriate SQL

**Interactive Menu Options:**
```
0) FULL RESET - Delete ALL data (CAUTION!)
1) AUTHENTICATION MODULE - OTP, Telegram, JWT data
2) USER MANAGEMENT MODULE - Profiles, audit trails
3) COMPANY MANAGEMENT MODULE - Companies, employees
4) PRICING MODULE - Delivery pricing rules
5) DELIVERY PACKAGE MODULE - Package tracking (future)
```

**Example Usage:**
- Reset only pricing rules: Enter `4`
- Reset auth + pricing: Enter `1 4`
- Full reset: Enter `0`
- Cancel: Enter `q`

**Verification:** The script shows PRESERVED vs CLEARED status for each table after reset.

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