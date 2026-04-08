#!/bin/sh
set -eu

COMPOSE_FILE="${COMPOSE_FILE:-deployments/docker-compose/docker-compose.yml}"

echo "[pre-deploy] validating docker compose"
docker compose -f "${COMPOSE_FILE}" config >/dev/null

echo "[pre-deploy] validating migration rollback pairs"
sh deployments/scripts/migration_check.sh

echo "[pre-deploy] validating OpenAPI index"
node tests/contract/check_openapi.mjs

echo "[pre-deploy] validating release smoke script presence"
test -f tests/release/smoke_release.mjs

echo "[pre-deploy] validating critical rehearsal scripts"
test -f tests/integration/billing_entitlement_playback_flow.mjs
test -f tests/integration/entitlement_revoke_access_downgrade.mjs
test -f tests/integration/admin_audit_flow.mjs
test -f tests/integration/feed_publish_visibility.mjs

echo "[pre-deploy] reminders"
echo "  - confirm database backup / snapshot exists"
echo "  - confirm staging smoke tests passed"
echo "  - confirm release readiness checklist is signed off"
echo "  - confirm release manifest is attached with impacted services and rollback target"
