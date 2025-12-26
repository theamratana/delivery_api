#!/usr/bin/env bash
set -euo pipefail

# Defaults for Docker Postgres mapping (host 5433 -> container 5432)
# The /api prefix is configured in WebConfig.java via addPathPrefix()
export DB_URL=${DB_URL:-jdbc:postgresql://localhost:5433/deliverydb}
export DB_USERNAME=${DB_USERNAME:-postgres}
export DB_PASSWORD=${DB_PASSWORD:-postgres}
export SERVER_PORT=${SERVER_PORT:-8081}

show_help() {
	cat <<EOF
Usage: ./run-api.sh [command]

Commands:
  start         Start API (uses existing Docker image if available)
  stop          Stop API and containers
  restart       Stop then start API
  status        Check if API is running
  rebuild       Quick rebuild with latest code (uses cache, faster) 🔄
  start-clean   Full clean rebuild (no cache, guaranteed fresh) 🧹
  clean         Alias for start-clean
  debug         Start API in debug mode (port 5005) with Docker PostgreSQL 🐛
  help          Show this help message

Examples:
  ./run-api.sh start          # Normal start
  ./run-api.sh rebuild        # After code changes (recommended)
  ./run-api.sh start-clean    # When rebuild doesn't work
  ./run-api.sh debug          # Debug mode (attach debugger to port 5005)
  ./run-api.sh stop           # Stop everything

Access API at: http://localhost:${SERVER_PORT}/api/
EOF
	exit 0
}

# Utilities
port_in_use() {
	local p="$1"
	if command -v netstat >/dev/null 2>&1; then
		netstat -an 2>/dev/null | grep -E "LISTEN|LISTENING" | grep -q ":${p}"
	else
		# Fallback for minimalist shells: try opening a TCP connection
		(echo > "/dev/tcp/127.0.0.1/${p}") >/dev/null 2>&1
	fi
}

kill_by_pid() {
	local pid="$1"
	if command -v powershell.exe >/dev/null 2>&1; then
		powershell.exe -NoProfile -Command "Stop-Process -Id $pid -Force" >/dev/null 2>&1 || true
	elif command -v taskkill >/dev/null 2>&1; then
		cmd.exe /c taskkill /F /PID "$pid" >/dev/null 2>&1 || true
	else
		kill -9 "$pid" >/dev/null 2>&1 || true
	fi
}

kill_on_port() {
	local p="$1"
	if [ -f .pid_api ]; then
		local pid=$(cat .pid_api)
		if [ -n "$pid" ]; then
			kill_by_pid "$pid" || true
			rm -f .pid_api
		fi
	fi
	if command -v netstat >/dev/null 2>&1; then
		# Get unique PIDs listening on the port
		local pids
		if command -v awk >/dev/null 2>&1; then
			pids=$(netstat -ano 2>/dev/null | grep -E "LISTEN|LISTENING" | grep ":${p}" | awk '{print $NF}' | sort -u)
		else
			# Fallback: try PowerShell if awk isn't available
			if command -v powershell.exe >/dev/null 2>&1; then
				pids=$(powershell.exe -NoProfile -Command "(Get-NetTCPConnection -LocalPort ${p} -State Listen).OwningProcess" 2>/dev/null | tr -d '\r')
			else
				pids=""
			fi
		fi
		for pid in $pids; do
			[ -n "$pid" ] && kill_by_pid "$pid"
		done
	fi
}

# Wait up to N seconds for the port to become free. Returns 0 if free, 1 if timed out.
wait_for_port_free() {
	local p="$1"
	local timeout_seconds=${2:-10}
	local waited=0
	while port_in_use "$p"; do
		if [ "$waited" -ge "$timeout_seconds" ]; then
			return 1
		fi
		sleep 1
		waited=$((waited + 1))
	done
	return 0
}

cmd="${1:-start}"

# Check if Docker is available and containers are running
docker_available() {
	command -v docker >/dev/null 2>&1 && docker ps >/dev/null 2>&1
}

docker_api_running() {
	docker_available && docker ps --format '{{.Names}}' 2>/dev/null | grep -q "roluun-api"
}

case "$cmd" in
	help|--help|-h)
		show_help
		;;
	stop)
		# Try to stop Docker containers first
		if docker_available; then
			echo "Stopping Docker containers..."
			docker compose down 2>/dev/null || echo "⚠️  No Docker containers to stop"
		fi
		# Also stop any local process on the port
		echo "Stopping API server on port ${SERVER_PORT}..."
		if port_in_use "$SERVER_PORT"; then
			kill_on_port "$SERVER_PORT"
			# Wait a few seconds for the port to be released
			if wait_for_port_free "$SERVER_PORT" 15; then
				echo "✅ API server stopped successfully."
			else
				echo "⚠️  Port ${SERVER_PORT} still appears in use after stop; you may need to investigate." >&2
			fi
		else
			echo "ℹ️  No API server running on port ${SERVER_PORT}."
		fi
		;;
	status)
		# Check Docker first
		if docker_api_running; then
			echo "✅ API running in Docker (container: roluun-api)"
			echo "   Access at: http://localhost:${SERVER_PORT}/api/"
			exit 0
		elif port_in_use "$SERVER_PORT"; then
			echo "✅ Port ${SERVER_PORT} is IN USE (API server running locally)"
			exit 0
		else
			echo "ℹ️  Port ${SERVER_PORT} is FREE (no API server running)"
			exit 1
		fi
		;;
	restart)
		echo "Restarting API server..."
		"$0" stop || true
		# ensure the port is free before starting
		if ! wait_for_port_free "$SERVER_PORT" 15; then
			echo "⚠️  Port ${SERVER_PORT} didn't free in time; proceeding to start may fail." >&2
		fi
		sleep 1
		"$0" start
		;;
	rebuild)
		# Quick rebuild (uses Gradle cache, faster)
		if docker_available; then
			echo "🔄 Rebuilding with latest code..."
			echo "   Step 1: Stopping API container..."
			docker compose stop api
			echo "   Step 2: Building Gradle (incremental)..."
			./gradlew build -x test
			echo "   Step 3: Rebuilding Docker image..."
			docker compose build api
			echo "   Step 4: Starting containers..."
			docker compose up -d postgres api
			echo "✅ Docker containers started with latest code"
			echo "   Waiting for API to be ready..."
			sleep 10
			if curl -s http://localhost:${SERVER_PORT}/api/auth/dev/token/00000000-0000-0000-0000-000000000000 >/dev/null 2>&1; then
				echo "✅ API is ready!"
				echo "   Access at: http://localhost:${SERVER_PORT}/api/"
			else
				echo "⚠️  API started but may still be initializing. Check logs: docker compose logs api"
			fi
			exit 0
		else
			echo "❌ Docker not available. Use './run-api.sh restart' for local mode."
			exit 1
		fi
		;;
	start-clean|clean)
		# Force full clean rebuild (no cache, slower but guaranteed fresh)
		if docker_available; then
			echo "🧹 Cleaning and rebuilding Docker image with latest code..."
			echo "   Step 1: Stopping containers..."
			docker compose down
			echo "   Step 2: Building Gradle (clean)..."
			./gradlew clean build -x test
			echo "   Step 3: Rebuilding Docker image (no cache)..."
			docker compose build --no-cache api
			echo "   Step 4: Starting containers..."
			docker compose up -d postgres api
			echo "✅ Docker containers started with fresh build"
			echo "   Waiting for API to be ready..."
			sleep 10
			if curl -s http://localhost:${SERVER_PORT}/api/auth/dev/token/00000000-0000-0000-0000-000000000000 >/dev/null 2>&1; then
				echo "✅ API is ready!"
				echo "   Access at: http://localhost:${SERVER_PORT}/api/"
			else
				echo "⚠️  API started but may still be initializing. Check logs: docker compose logs api"
			fi
			exit 0
		else
			echo "❌ Docker not available. Use './run-api.sh restart' for local mode."
			exit 1
		fi
		;;
	debug)
		# Start in debug mode with Docker PostgreSQL
		echo "🐛 Starting API in DEBUG MODE..."
		
		# Ensure Docker PostgreSQL is running
		if docker_available; then
			echo "   Step 1: Starting PostgreSQL container..."
			docker compose up -d postgres
			sleep 3
		else
			echo "❌ Docker not available. Cannot start PostgreSQL."
			exit 1
		fi
		
		# Stop any existing API process
		if port_in_use "$SERVER_PORT"; then
			echo "   Step 2: Stopping existing API process..."
			kill_on_port "$SERVER_PORT"
			wait_for_port_free "$SERVER_PORT" 10
		fi
		
		echo "   Step 3: Building project..."
		./gradlew build -x test
		
		echo "   Step 4: Starting API with remote debugging..."
		echo "   🐛 Debug port: 5005"
		echo "   🌐 API port: ${SERVER_PORT}"
		echo "   📍 Attach your debugger to localhost:5005"
		echo ""
		echo "   Starting in foreground (Ctrl+C to stop)..."
		echo "================================================"
		
		# Run with debug JVM options
		./gradlew bootRun --args="--debug" -Dorg.gradle.jvmargs="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
		;;
	start|*)
		# Check if Docker is available and start via Docker
		if docker_available; then
			echo "🐳 Docker detected, starting API via docker-compose..."
			docker compose up -d postgres api
			echo "✅ Docker containers started"
			echo "   Waiting for API to be ready..."
			sleep 10
			if curl -s http://localhost:${SERVER_PORT}/api/auth/dev/token/00000000-0000-0000-0000-000000000000 >/dev/null 2>&1; then
				echo "✅ API is ready!"
				echo "   Access at: http://localhost:${SERVER_PORT}/api/"
			else
				echo "⚠️  API started but may still be initializing. Check logs: docker compose logs api"
			fi
			exit 0
		fi
		
		# Fallback to local Gradle run if Docker not available
		# Ensure port is free (stop any existing process first)
		if port_in_use "$SERVER_PORT"; then
			echo "Port ${SERVER_PORT} is busy. Stopping existing process..."
			kill_on_port "$SERVER_PORT"
			if ! wait_for_port_free "$SERVER_PORT" 15; then
				echo "❌ ERROR: Could not free port ${SERVER_PORT}." >&2
				exit 1
			fi
		fi
		
		echo "🚀 Starting API server on port ${SERVER_PORT}..."
		echo "   Database: ${DB_URL}"
		# Choose a launch mechanism that survives shell exit if available
		if [ -x "./gradlew" ]; then
			LAUNCH_CMD=("./gradlew" "bootRun")
		else
			LAUNCH_CMD=("./gradle-8.5/bin/gradle" "bootRun")
		fi
		# Redirect logs to .api.log for easy inspection
		if command -v nohup >/dev/null 2>&1; then
			nohup "${LAUNCH_CMD[@]}" > .api.log 2>&1 &
			echo $! > .pid_api
		elif command -v setsid >/dev/null 2>&1; then
			setsid "${LAUNCH_CMD[@]}" > .api.log 2>&1 &
			echo $! > .pid_api
		else
			"${LAUNCH_CMD[@]}" > .api.log 2>&1 &
			echo $! > .pid_api
		fi
		PID=$(cat .pid_api)
		echo "✅ API server started (PID: ${PID}). Logs -> .api.log"
		echo "   Access at: http://localhost:${SERVER_PORT}/api/"
		;;
esac
