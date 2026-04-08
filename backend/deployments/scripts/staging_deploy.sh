#!/bin/sh
set -eu

COMPOSE_FILE="${COMPOSE_FILE:-deployments/docker-compose/docker-compose.yml}"

echo "[staging-deploy] start"
echo "[staging-deploy] validating candidate"
sh deployments/scripts/pre_deploy_check.sh

echo "[staging-deploy] deploy reminder"
echo "  - load staging env from backend/.env.staging.example backed by real secrets"
echo "  - apply migrations only after backup/snapshot confirmation"
echo "  - deploy affected services only, then run post deploy verify"

docker compose -f "${COMPOSE_FILE}" config >/dev/null

echo "[staging-deploy] running post-deploy verify placeholder"
sh deployments/scripts/post_deploy_verify.sh

echo "[staging-deploy] run release smoke next:"
echo "  DRAMAFLOW_RUN_INTEGRATION=1 node backend/tests/release/smoke_release.mjs"
