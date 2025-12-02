# Postgres Access — ROLUUN (Development)

This repository runs Postgres inside Docker for local development.

## Summary (current configuration)
- Image: postgres:16
- Container name: `delivery-postgres`
- Hostname inside Docker network: `roluun-db`
- Database: `deliverydb`
- Username: `postgres`
- Password: `postgres`
- Host port mapped: `5433` -> container `5432` (so host can connect on localhost:5433)

## Ways to access the DB

### 1) Use psql from host (Windows)
If you have a local Postgres client (psql) installed, connect like:

```powershell
psql -h localhost -p 5433 -U postgres -d deliverydb
# When prompted, enter password: postgres
```

If `psql` isn't installed on your Windows host, install it via Postgres installer or use the Docker helper (next option).

### 2) Use the helper script (docker exec psql)
This will open an interactive psql session inside the running postgres container.

On Linux/macOS (or Git Bash on Windows):

```bash
./db-shell.sh
# or with container name if changed
./db-shell.sh delivery-postgres postgres deliverydb
```

On Windows cmd/powershell:

```cmd
.\db-shell.bat
# or
.\db-shell.bat delivery-postgres
```

This runs:
```bash
docker exec -it delivery-postgres psql -U postgres -d deliverydb
```

### 3) Use Adminer (web UI)
This repository includes an Adminer service that runs inside the compose network and is pre-configured to connect to the Postgres service.

Start Docker and then open Adminer in a browser:

```
http://localhost:8888
```

Adminer is pre-filled to connect to the DB service automatically, but here are the manual values if you need them:
- System: PostgreSQL
- Server: `roluun-db` (recommended when Adminer is running in Docker)
- Host from the host machine: `localhost:5433` (if you want to connect to Postgres via the host mapping)
- Username: `postgres`
- Password: `postgres`
- Database: `deliverydb`

> Note: When connecting from the host browser, set server to `localhost:5433`.
> When connecting from Adminer inside the same Docker network, use `roluun-db:5432`.

### 4) (Optional) Use pgAdmin or other GUI
If you already prefer a GUI for Postgres you'll have two choices added by the repository:

- Adminer (lightweight, web-based) — available at port 8080
- Use pgAdmin locally on your machine

If you run pgAdmin on your desktop, connect like this:

 - Host: localhost
 - Port: 5433
 - Database: deliverydb
 - Username: postgres
 - Password: postgres

## Troubleshooting

- Docker must be running (Docker Desktop on Windows). If `docker-compose` or `docker` commands fail, start Docker Desktop.
- Confirm containers are up:

```bash
docker-compose ps
```

- If `psql` connection refused on port 5433, check the postgres container logs:

```bash
docker-compose logs --tail=200 postgres
```

- If you get authentication errors, verify `POSTGRES_USER` and `POSTGRES_PASSWORD` in `docker-compose.yml`

## Security Note
These credentials are for local development only. Do NOT use these credentials in production.

---

If you'd like, I can also:
- Add a lightweight SQL script to pre-populate test data.
- Open the DB to remote connections (not recommended unless behind VPN/firewall).

Tell me which you'd like next (data seed, or remote secure access), and I'll implement it.