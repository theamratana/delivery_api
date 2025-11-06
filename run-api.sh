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

cmd="${1:-start}"

case "$cmd" in
	stop)
		echo "Stopping anything bound to :${SERVER_PORT}..."
		kill_on_port "$SERVER_PORT"
		echo "Done."
		;;
	status)
		if port_in_use "$SERVER_PORT"; then
			echo "Port ${SERVER_PORT} is IN USE"
			exit 0
		else
			echo "Port ${SERVER_PORT} is FREE"
			exit 1
		fi
		;;
	restart)
		"$0" stop || true
		exec "$0" start
		;;
	start|*)
		# Ensure only 8081 is used: free it first if needed
		if port_in_use "$SERVER_PORT"; then
			echo "Port ${SERVER_PORT} is busy. Attempting to stop existing process..."
			kill_on_port "$SERVER_PORT"
			sleep 1
			if port_in_use "$SERVER_PORT"; then
				echo "ERROR: Could not free port ${SERVER_PORT}." >&2
				exit 1
			fi
		fi
			echo "Starting API on port ${SERVER_PORT}..."
			if [ -x "./gradlew" ]; then
				./gradlew bootRun &
				echo $! > .pid_api
			else
				./gradle-8.5/bin/gradle bootRun &
				echo $! > .pid_api
			fi
		;;
esac
