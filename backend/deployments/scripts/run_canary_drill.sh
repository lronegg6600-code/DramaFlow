#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "${ROOT_DIR}"

COMPOSE_FILE="${COMPOSE_FILE:-deployments/docker-compose/docker-compose.yml}"
TARGET_SERVICE="${TARGET_SERVICE:-admin-service}"
WINDOW_MINUTES="${WINDOW_MINUTES:-15}"
RUN_KIND="${DRAMAFLOW_REAL_RUN:-0}"
CONTROL_PLANE="${DRAMAFLOW_CONTROL_PLANE:-docker-compose}"
EVIDENCE_DIR="${DRAMAFLOW_EVIDENCE_DIR:-${ROOT_DIR}/release-evidence/canary}"

mkdir -p "${EVIDENCE_DIR}"
echo "[canary-drill] target_service=${TARGET_SERVICE}" | tee "${EVIDENCE_DIR}/canary-drill.log"
echo "[canary-drill] control_plane=${CONTROL_PLANE}" | tee -a "${EVIDENCE_DIR}/canary-drill.log"
echo "[canary-drill] real_run=${RUN_KIND}" | tee -a "${EVIDENCE_DIR}/canary-drill.log"

if [ "${RUN_KIND}" != "1" ]; then
  echo "[canary-drill] dry-run only; rollout skipped" | tee -a "${EVIDENCE_DIR}/canary-drill.log"
elif [ "${CONTROL_PLANE}" = "docker-compose" ]; then
  echo "[canary-drill] local compose only supports single-instance surrogate canary" | tee -a "${EVIDENCE_DIR}/canary-drill.log"
  docker compose -f "${COMPOSE_FILE}" up --build -d "${TARGET_SERVICE}"
elif [ "${CONTROL_PLANE}" = "kubernetes" ]; then
  kubectl rollout status deployment/"${TARGET_SERVICE}" -n "${DRAMAFLOW_KUBE_NAMESPACE:-dramaflow-staging}"
else
  echo "[canary-drill] unsupported control plane: ${CONTROL_PLANE}" >&2
  exit 1
fi

WINDOW_MINUTES="${WINDOW_MINUTES}" sh deployments/scripts/canary_check.sh
sh deployments/scripts/release_watch.sh
node tests/release/verify_canary_gate.mjs

echo "canary_result=pass" >> "${EVIDENCE_DIR}/canary-drill.log"
echo "[canary-drill] complete"
