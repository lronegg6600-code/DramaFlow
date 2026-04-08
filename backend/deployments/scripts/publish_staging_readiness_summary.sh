#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "${ROOT_DIR}"

EVIDENCE_DIR="${DRAMAFLOW_EVIDENCE_DIR:-${ROOT_DIR}/release-evidence}"
OUT_FILE="${EVIDENCE_DIR}/staging-readiness-summary.md"

mkdir -p "${EVIDENCE_DIR}"

node tests/release/verify_staging_unblock.mjs || true

cat > "${OUT_FILE}" <<EOF
# Staging Readiness Summary

- generated_at: $(date -u +"%Y-%m-%dT%H:%M:%SZ")
- evidence_dir: ${EVIDENCE_DIR}
- source_json:
  - platform-input-intake.json
  - staging-unblock-summary.json
  - real-repo-identity.json
  - real-artifact-identity.json
  - real-cluster-access.json
  - real-github-environments.json
  - real-secret-readiness.json
EOF

echo "[publish-staging-readiness-summary] wrote ${OUT_FILE}"
