#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "${ROOT_DIR}"

TARGET_SERVICE="${TARGET_SERVICE:-admin-service}"
COMPOSE_FILE="${COMPOSE_FILE:-deployments/docker-compose/docker-compose.yml}"
RUN_KIND="${DRAMAFLOW_REAL_RUN:-0}"
CONTROL_PLANE="${DRAMAFLOW_CONTROL_PLANE:-docker-compose}"
EVIDENCE_DIR="${DRAMAFLOW_EVIDENCE_DIR:-${ROOT_DIR}/release-evidence/rollback}"

mkdir -p "${EVIDENCE_DIR}"
echo "[rollback-drill] target_service=${TARGET_SERVICE}" | tee "${EVIDENCE_DIR}/rollback-drill.log"
echo "[rollback-drill] control_plane=${CONTROL_PLANE}" | tee -a "${EVIDENCE_DIR}/rollback-drill.log"
echo "[rollback-drill] real_run=${RUN_KIND}" | tee -a "${EVIDENCE_DIR}/rollback-drill.log"

if [ "${RUN_KIND}" != "1" ]; then
  echo "[rollback-drill] dry-run only; rollback skipped" | tee -a "${EVIDENCE_DIR}/rollback-drill.log"
elif [ "${CONTROL_PLANE}" = "docker-compose" ]; then
  echo "[rollback-drill] executing restart-based rollback surrogate" | tee -a "${EVIDENCE_DIR}/rollback-drill.log"
  docker compose -f "${COMPOSE_FILE}" restart "${TARGET_SERVICE}"
elif [ "${CONTROL_PLANE}" = "kubernetes" ]; then
  kubectl rollout undo deployment/"${TARGET_SERVICE}" -n "${DRAMAFLOW_KUBE_NAMESPACE:-dramaflow-staging}"
  kubectl rollout status deployment/"${TARGET_SERVICE}" -n "${DRAMAFLOW_KUBE_NAMESPACE:-dramaflow-staging}"
else
  echo "[rollback-drill] unsupported control plane: ${CONTROL_PLANE}" >&2
  exit 1
fi

sh deployments/scripts/rollback_verify.sh
node tests/release/verify_rollback_gate.mjs
DRAMAFLOW_RUN_INTEGRATION=1 node tests/release/smoke_release.mjs

echo "rollback_result=pass" >> "${EVIDENCE_DIR}/rollback-drill.log"
echo "[rollback-drill] complete"
