#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "${ROOT_DIR}"

TARGET_ENV="${DRAMAFLOW_TARGET_ENV:-staging}"
EVIDENCE_DIR="${DRAMAFLOW_EVIDENCE_DIR:-${ROOT_DIR}/release-evidence/${TARGET_ENV}-enablement}"
REPORT_FILE="${EVIDENCE_DIR}/enablement-report.md"

mkdir -p "${EVIDENCE_DIR}"

STATUS="pass"
for script in \
  deployments/scripts/verify_repo_identity.sh \
  deployments/scripts/verify_artifact_identity.sh \
  deployments/scripts/verify_cluster_access.sh \
  deployments/scripts/verify_deploy_tooling.sh \
  deployments/scripts/verify_github_environment_setup.sh \
  deployments/scripts/verify_secret_readiness.sh
do
  if ! DRAMAFLOW_EVIDENCE_DIR="${EVIDENCE_DIR}" sh "${script}"; then
    STATUS="blocker"
  fi
done

cat > "${REPORT_FILE}" <<EOF
# Environment Enablement Report

- target_env: ${TARGET_ENV}
- generated_at: $(date -u +"%Y-%m-%dT%H:%M:%SZ")
- status: ${STATUS}
- evidence_dir: ${EVIDENCE_DIR}

Included evidence files:

- repo-identity.json
- artifact-identity.json
- cluster-access.json
- deploy-tooling.json
- github-environments.json
- secret-inventory.json
EOF

if [ "${STATUS}" != "pass" ]; then
  echo "[export-enablement-report] blocker"
  exit 1
fi

echo "[export-enablement-report] pass"
