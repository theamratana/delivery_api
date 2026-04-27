#!/bin/bash
# deployFlutter.sh — runs ON the server
# Usage: ./deployFlutter.sh [path-to-web_build.tar.gz]
#   Default: /tmp/web_build.tar.gz

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ARCHIVE="${1:-$SCRIPT_DIR/web_build.tar.gz}"
DEPLOY_DIR="/opt/roluun/flutter-web"

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

# Reload Nginx
echo "[3/3] Reloading Nginx..."
docker exec roluun-nginx nginx -s reload

echo ""
echo "Done! http://app.roluun.com"
