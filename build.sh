#!/usr/bin/env bash
# start.sh — build (if needed) and start the CDC CHCCLP REST service
set -euo pipefail

JAR="target/cdc-chcclp-rest-server-1.0-SNAPSHOT.jar"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "$SCRIPT_DIR"

mvn -q package -DskipTests
