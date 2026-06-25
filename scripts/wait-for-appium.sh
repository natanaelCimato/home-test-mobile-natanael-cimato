#!/usr/bin/env bash
set -euo pipefail

STATUS_URL="${1:-http://127.0.0.1:4723/status}"
TIMEOUT_SECONDS="${2:-300}"
DEADLINE=$((SECONDS + TIMEOUT_SECONDS))

echo "Waiting for Appium at ${STATUS_URL}"

while (( SECONDS < DEADLINE )); do
  if command -v curl >/dev/null 2>&1 && curl --silent --fail --max-time 5 "${STATUS_URL}" >/dev/null; then
    echo "Appium is reachable."
    exit 0
  fi

  sleep 5
done

echo "Timed out waiting for Appium after ${TIMEOUT_SECONDS} seconds." >&2
exit 1
