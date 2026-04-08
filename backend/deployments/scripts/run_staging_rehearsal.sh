#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "${ROOT_DIR}"

COMPOSE_FILE="${COMPOSE_FILE:-deployments/docker-compose/docker-compose.yml}"
REHEARSAL_MODE="${REHEARSAL_MODE:-local-compose-surrogate}"
RUN_KIND="${DRAMAFLOW_REAL_RUN:-0}"
TARGET_ENV="${DRAMAFLOW_TARGET_ENV:-staging}"
CONTROL_PLANE="${DRAMAFLOW_CONTROL_PLANE:-docker-compose}"
EVIDENCE_DIR="${DRAMAFLOW_EVIDENCE_DIR:-${ROOT_DIR}/release-evidence/staging}"

mkdir -p "${EVIDENCE_DIR}"

echo "[staging-rehearsal] target_env=${TARGET_ENV}" | tee "${EVIDENCE_DIR}/staging-rehearsal.log"
echo "[staging-rehearsal] control_plane=${CONTROL_PLANE}" | tee -a "${EVIDENCE_DIR}/staging-rehearsal.log"
echo "[staging-rehearsal] mode=${REHEARSAL_MODE}" | tee -a "${EVIDENCE_DIR}/staging-rehearsal.log"
echo "[staging-rehearsal] real_run=${RUN_KIND}" | tee -a "${EVIDENCE_DIR}/staging-rehearsal.log"

DRAMAFLOW_EVIDENCE_DIR="${EVIDENCE_DIR}" node tests/release/collect_release_metadata.mjs >/dev/null

echo "[staging-rehearsal] pre-deploy"
sh deployments/scripts/pre_deploy_check.sh

if [ "${RUN_KIND}" != "1" ]; then
  echo "[staging-rehearsal] dry-run only; deploy skipped" | tee -a "${EVIDENCE_DIR}/staging-rehearsal.log"
else
  echo "[staging-rehearsal] deploy/upgrade"
  if [ "${CONTROL_PLANE}" = "docker-compose" ]; then
    docker compose -f "${COMPOSE_FILE}" up --build -d admin-service billing-service entitlement-service playback-service
  elif [ "${CONTROL_PLANE}" = "kubernetes" ]; then
    kubectl rollout status deployment/admin-service -n "${DRAMAFLOW_KUBE_NAMESPACE:-dramaflow-staging}"
  else
    echo "[staging-rehearsal] unsupported control plane: ${CONTROL_PLANE}" >&2
    exit 1
  fi
fi

echo "[staging-rehearsal] post-deploy verify"
sh deployments/scripts/post_deploy_verify.sh

echo "[staging-rehearsal] release smoke"
DRAMAFLOW_RUN_INTEGRATION=1 node tests/release/smoke_release.mjs

echo "[staging-rehearsal] critical flows"
DRAMAFLOW_RUN_INTEGRATION=1 node tests/integration/billing_entitlement_playback_flow.mjs
DRAMAFLOW_RUN_INTEGRATION=1 node tests/integration/entitlement_revoke_access_downgrade.mjs
DRAMAFLOW_RUN_INTEGRATION=1 node tests/integration/admin_audit_flow.mjs
DRAMAFLOW_RUN_INTEGRATION=1 node tests/integration/feed_publish_visibility.mjs

echo "[staging-rehearsal] final gate"
node tests/release/verify_staging_gate.mjs

echo "staging_rehearsal_result=pass" >> "${EVIDENCE_DIR}/staging-rehearsal.log"
echo "[staging-rehearsal] complete"
