#!/usr/bin/env bash
# start.sh — build (if needed) and start the CDC CHCCLP REST service
set -euo pipefail

JAR="target/cdc-chcclp-rest-server-1.0-SNAPSHOT.jar"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "$SCRIPT_DIR"

# ── Build if the fat-jar is missing ──────────────────────────────────────────
if [[ ! -f "$JAR" ]]; then
  echo "JAR not found – building with Maven…"
  ${SCRIPT_DIR}/build.sh
fi

# ── Start the service ─────────────────────────────────────────────────────────
echo "Starting CDC CHCCLP REST service (port 8080)…"
exec java -jar "$JAR"
