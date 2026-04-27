#!/bin/bash
# deployFlutter.sh — runs ON the server
# Usage: ./deployFlutter.sh [path-to-web_build.tar.gz]
#   Default: /tmp/web_build.tar.gz

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ARCHIVE="${1:-$SCRIPT_DIR/web_build.tar.gz}"
DEPLOY_DIR="/opt/roluun/flutter-web"
COMPOSE_DIR="/opt/roluun"

echo "=== Flutter Web Deploy ==="
echo "Archive:    $ARCHIVE"
echo "Deploy dir: $DEPLOY_DIR"
echo ""

# Check archive exists
if [ ! -f "$ARCHIVE" ]; then
    echo "ERROR: Archive not found at $ARCHIVE"
    exit 1
fi

# Clear old files
echo "[1/3] Clearing old files..."
rm -rf "${DEPLOY_DIR:?}"/*

# Extract
echo "[2/3] Extracting..."
tar -xzf "$ARCHIVE" -C "$DEPLOY_DIR"
rm -f "$ARCHIVE"

# Reload or restart Nginx
echo "[3/3] Reloading Nginx..."
if docker exec roluun-nginx nginx -s reload 2>/dev/null; then
    echo "Nginx reloaded."
else
    echo "Nginx not running, restarting container..."
    cd "$COMPOSE_DIR" && docker compose up -d nginx
fi

echo ""
echo "Done! http://app.roluun.com"
