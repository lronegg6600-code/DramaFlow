#!/bin/sh
set -eu

BASE_URL="${BASE_URL:-http://localhost}"

echo "[rollback-verify] checking core health endpoints"
curl -fsS "${BASE_URL}:8085/health/live" >/dev/null
curl -fsS "${BASE_URL}:8086/health/live" >/dev/null
curl -fsS "${BASE_URL}:8087/health/live" >/dev/null
curl -fsS "${BASE_URL}:8088/health/live" >/dev/null

echo "[rollback-verify] next required actions:"
echo "  - rerun release smoke"
echo "  - check release watchlist for 15 minutes"
echo "  - confirm incident timeline and rollback target in decision log"
