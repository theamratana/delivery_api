#!/usr/bin/env bash
# =============================================================================
# deploy.sh — Roluun Delivery API deployment helper
# =============================================================================
# Usage:  ./deploy.sh <command> [options]
#
# Commands:
#   start              Start all services
#   stop               Stop all services
#   restart            Stop + start  (applies .env changes)
#   rebuild            Rebuild Docker image + start  (applies code changes)
#   status             Show running containers
#   logs [service]     Tail logs  (service: api | db | all  — default: api)
#   backup             Dump database → ./backups/
#   restore <file>     Restore database from a backup file
#   help               Show this help
# =============================================================================
set -euo pipefail

COMPOSE_FILE="docker-compose.prod.yml"
BACKUP_DIR="./backups"

# ── Colors ────────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log()  { echo -e "${GREEN}[$(date '+%H:%M:%S')]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error(){ echo -e "${RED}[ERROR]${NC} $1" >&2; exit 1; }
info() { echo -e "${CYAN}$1${NC}"; }

# ── Guards ────────────────────────────────────────────────────────────────────
require_env() {
    [[ -f ".env" ]] || error ".env not found.\n  Run: cp .env.example .env && nano .env"
}

require_compose() {
    docker compose version >/dev/null 2>&1 || \
    error "docker compose not found. Install Docker Engine with Compose plugin."
}

# Safely read a key from .env without sourcing the whole file
env_val() {
    grep -E "^${1}=" .env 2>/dev/null | head -1 | cut -d= -f2- | tr -d '"' | tr -d "'"
}

# ── Commands ──────────────────────────────────────────────────────────────────

cmd_start() {
    require_env
    require_compose
    log "Starting Roluun API..."
    docker compose -f "$COMPOSE_FILE" up -d
    PORT=$(env_val SERVER_PORT); PORT="${PORT:-8081}"
    log "Done. API → http://$(hostname -I | awk '{print $1}'):${PORT}/api/"
}

cmd_stop() {
    require_compose
    log "Stopping Roluun API..."
    docker compose -f "$COMPOSE_FILE" down
    log "Stopped."
}

cmd_restart() {
    log "Restarting (applies .env changes)..."
    cmd_stop
    cmd_start
}

cmd_rebuild() {
    require_env
    require_compose
    log "Rebuilding Docker image (this may take a few minutes)..."
    docker compose -f "$COMPOSE_FILE" down
    docker compose -f "$COMPOSE_FILE" build --no-cache api
    docker compose -f "$COMPOSE_FILE" up -d
    log "Rebuild complete."
}

cmd_status() {
    require_compose
    docker compose -f "$COMPOSE_FILE" ps
}

cmd_logs() {
    require_compose
    SERVICE="${1:-api}"
    case "$SERVICE" in
        all)      docker compose -f "$COMPOSE_FILE" logs -f ;;
        db|postgres) docker compose -f "$COMPOSE_FILE" logs -f postgres ;;
        *)        docker compose -f "$COMPOSE_FILE" logs -f "$SERVICE" ;;
    esac
}

cmd_backup_db() {
    require_env
    require_compose

    DB_USER=$(env_val DB_USERNAME); DB_USER="${DB_USER:-postgres}"
    DB_NAME=$(env_val DB_NAME);     DB_NAME="${DB_NAME:-deliverydb}"
    DB_PASS=$(env_val DB_PASSWORD); DB_PASS="${DB_PASS:-postgres}"

    mkdir -p "$BACKUP_DIR"
    TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
    BACKUP_FILE="${BACKUP_DIR}/db_backup_${TIMESTAMP}.sql.gz"

    log "Backing up database '${DB_NAME}' → ${BACKUP_FILE} ..."
    docker exec -e PGPASSWORD="$DB_PASS" roluun-db pg_dump -h 127.0.0.1 -U "$DB_USER" "$DB_NAME" | gzip > "$BACKUP_FILE"
    log "Database backup saved: ${BACKUP_FILE}"
}

cmd_backup_uploads() {
    mkdir -p "$BACKUP_DIR"
    TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
    BACKUP_FILE="${BACKUP_DIR}/uploads_backup_${TIMESTAMP}.tar.gz"

    [[ -d "./uploads" ]] || error "./uploads directory not found."

    log "Backing up uploads → ${BACKUP_FILE} ..."
    tar -czf "$BACKUP_FILE" -C . uploads
    log "Uploads backup saved: ${BACKUP_FILE}"
}

cmd_backup_all() {
    log "Running full backup (database + uploads)..."
    cmd_backup_db
    cmd_backup_uploads
    log "Full backup complete."
}

cmd_restore() {
    FILE="${1:-}"
    [[ -n "$FILE" ]] || error "Usage: ./deploy.sh restore <backup_file>"
    [[ -f "$FILE"  ]] || error "File not found: $FILE"
    require_env
    require_compose

    DB_USER=$(env_val DB_USERNAME); DB_USER="${DB_USER:-postgres}"
    DB_NAME=$(env_val DB_NAME);     DB_NAME="${DB_NAME:-deliverydb}"

    warn "This will OVERWRITE the database '${DB_NAME}'. Continue? [y/N]"
    read -r confirm
    [[ "$confirm" =~ ^[yY]$ ]] || { log "Cancelled."; exit 0; }

    log "Restoring from ${FILE} ..."
    gunzip -c "$FILE" | docker exec -i roluun-db psql -U "$DB_USER" "$DB_NAME"
    log "Restore complete."
}

show_help() {
    cat <<EOF

${CYAN}Roluun Delivery API — deploy.sh${NC}

Usage: ./deploy.sh <command> [options]

Commands:
  start              Start all services (postgres + api)
  stop               Stop all services
  restart            Stop + start  — use after editing .env
  rebuild            Rebuild image + start  — use after pulling new code
  status             Show container status
  logs [service]     Tail logs  (api | db | all   default: api)
  backup-db          Dump database → ./backups/
  backup-uploads     Archive ./uploads folder → ./backups/
  backup-all         Both database + uploads
  restore <file>     Restore database from a backup file
  help               Show this help

First-time setup on a new server:
  1. Install Docker:     https://docs.docker.com/engine/install/ubuntu/
  2. Clone the repo or copy project files to the server
  3. Copy uploads:       rsync -avz ./uploads/ user@server:/path/to/project/uploads/
  4. Create .env:        cp .env.example .env && nano .env
  5. Start:              ./deploy.sh start
  6. (optional) Restore backup:  ./deploy.sh restore ./backups/db_backup_*.sql.gz

Workflow:
  Edit .env → ./deploy.sh restart         (config change)
  git pull  → ./deploy.sh rebuild         (code change)
  ./deploy.sh backup-all                  (before any risky change)

EOF
}

# ── Router ────────────────────────────────────────────────────────────────────
COMMAND="${1:-help}"
shift || true

case "$COMMAND" in
    start)   cmd_start ;;
    stop)    cmd_stop ;;
    restart) cmd_restart ;;
    rebuild) cmd_rebuild ;;
    status)  cmd_status ;;
    logs)    cmd_logs "${1:-api}" ;;
    backup-db|backup|db-backup) cmd_backup_db ;;
    backup-uploads|backup-files) cmd_backup_uploads ;;
    backup-all) cmd_backup_all ;;
    restore) cmd_restore "${1:-}" ;;
    help|-h|--help) show_help ;;
    *) error "Unknown command: '$COMMAND'  →  run './deploy.sh help'" ;;
esac
