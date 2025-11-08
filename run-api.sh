#!/usr/bin/env bash
set -euo pipefail

# Defaults for local Docker Postgres mapping (host 5433 -> container 5432)
export DB_URL=${DB_URL:-jdbc:postgresql://localhost:5433/deliverydb}
export DB_USERNAME=${DB_USERNAME:-postgres}
export DB_PASSWORD=${DB_PASSWORD:-postgres}
export SERVER_PORT=8081

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

case "$cmd" in
	stop)
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
		if port_in_use "$SERVER_PORT"; then
			echo "✅ Port ${SERVER_PORT} is IN USE (API server running)"
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
	start|*)
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
		# Choose a launch mechanism that survives shell exit if available
		if [ -x "./gradlew" ]; then
			LAUNCH_CMD=("./gradlew" "bootRun")
		else
			LAUNCH_CMD=("./gradle-8.5/bin/gradle" "bootRun")
		fi
		# redirect logs to .api.log for easy inspection
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
		echo "✅ API server started (PID: $(cat .pid_api)). Logs -> .api.log"
		;;
esac
