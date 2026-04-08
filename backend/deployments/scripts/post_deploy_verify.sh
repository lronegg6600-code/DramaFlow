#!/bin/sh
set -eu

BASE_URL="${BASE_URL:-http://localhost}"

check() {
  url="$1"
  echo "[post-deploy] checking ${url}"
  curl -fsS "${url}" >/dev/null
}

check "${BASE_URL}:8081/health/live"
check "${BASE_URL}:8082/health/live"
check "${BASE_URL}:8083/health/live"
check "${BASE_URL}:8084/health/live"
check "${BASE_URL}:8085/health/live"
check "${BASE_URL}:8086/health/live"
check "${BASE_URL}:8087/health/live"
check "${BASE_URL}:8088/health/live"
check "${BASE_URL}:3000/login"

echo "[post-deploy] next actions"
echo "  - run release smoke: DRAMAFLOW_RUN_INTEGRATION=1 node backend/tests/release/smoke_release.mjs"
echo "  - run release watch: sh backend/deployments/scripts/release_watch.sh"
echo "[post-deploy] ok"
