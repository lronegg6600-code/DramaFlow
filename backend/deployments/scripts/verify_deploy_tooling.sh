#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "${ROOT_DIR}"

EVIDENCE_DIR="${DRAMAFLOW_EVIDENCE_DIR:-${ROOT_DIR}/release-evidence}"
OUT_FILE="${EVIDENCE_DIR}/deploy-tooling.json"
mkdir -p "${EVIDENCE_DIR}"

KUBECTL_PATH="$(command -v kubectl || true)"
HELM_PATH="$(command -v helm || true)"

BLOCKERS=""
if [ -z "${KUBECTL_PATH}" ]; then
  BLOCKERS="${BLOCKERS}\n- kubectl is missing"
fi
if [ -z "${HELM_PATH}" ]; then
  BLOCKERS="${BLOCKERS}\n- helm is missing"
fi
if [ ! -f "deployments/helm/dramaflow-backend/values.yaml" ]; then
  BLOCKERS="${BLOCKERS}\n- helm chart values.yaml is missing"
fi
if [ ! -f "deployments/helm/values/staging.values.example.yaml" ]; then
  BLOCKERS="${BLOCKERS}\n- staging values example is missing"
fi
if [ ! -f "deployments/helm/values/production.values.example.yaml" ]; then
  BLOCKERS="${BLOCKERS}\n- production values example is missing"
fi

cat > "${OUT_FILE}" <<EOF
{
  "category": "deploy_tooling",
  "kubectlPath": "${KUBECTL_PATH}",
  "helmPath": "${HELM_PATH}",
  "status": "$( [ -z "${BLOCKERS}" ] && printf pass || printf blocker )"
}
EOF

if [ -n "${BLOCKERS}" ]; then
  printf '%s\n' "[verify-deploy-tooling] blocker"
  printf '%b\n' "${BLOCKERS}"
  exit 1
fi

echo "[verify-deploy-tooling] pass"
