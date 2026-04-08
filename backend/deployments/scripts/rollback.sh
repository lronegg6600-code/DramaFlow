#!/bin/sh
set -eu

SERVICE="${1:-}"
MIGRATION_TARGET="${2:-}"

echo "[rollback] start"
echo "  service=${SERVICE:-all}"
echo "  migration_target=${MIGRATION_TARGET:-none}"

if [ -n "${MIGRATION_TARGET}" ]; then
  echo "[rollback] operator must run the matching rollback SQL for ${MIGRATION_TARGET}"
fi

if [ -n "${SERVICE}" ]; then
  docker compose -f deployments/docker-compose/docker-compose.yml restart "${SERVICE}"
else
  docker compose -f deployments/docker-compose/docker-compose.yml restart
fi

echo "[rollback] completed"
echo "[rollback] next required verification:"
echo "  sh deployments/scripts/rollback_verify.sh"
