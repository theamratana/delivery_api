#!/usr/bin/env bash
# Connect to Postgres running in Docker with psql
CONTAINER_NAME=${1:-delivery-postgres}
DB_USER=${2:-postgres}
DB_NAME=${3:-deliverydb}

if ! command -v docker >/dev/null 2>&1; then
  echo "docker not found in PATH. Ensure Docker Desktop is running on Windows or docker is installed."
  exit 1
fi

# Exec to psql in container
docker exec -it "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME"
