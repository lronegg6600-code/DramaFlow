#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "${ROOT_DIR}"

EVIDENCE_DIR="${DRAMAFLOW_EVIDENCE_DIR:-${ROOT_DIR}/release-evidence}"
mkdir -p "${EVIDENCE_DIR}"

echo "[go-no-go] collect release evidence"
DRAMAFLOW_EVIDENCE_DIR="${EVIDENCE_DIR}" sh deployments/scripts/collect_release_evidence.sh

echo "[go-no-go] environment readiness checks"
DRAMAFLOW_EVIDENCE_DIR="${EVIDENCE_DIR}" sh deployments/scripts/verify_repo_identity.sh
DRAMAFLOW_EVIDENCE_DIR="${EVIDENCE_DIR}" sh deployments/scripts/verify_artifact_identity.sh
DRAMAFLOW_EVIDENCE_DIR="${EVIDENCE_DIR}" sh deployments/scripts/verify_cluster_access.sh
DRAMAFLOW_EVIDENCE_DIR="${EVIDENCE_DIR}" sh deployments/scripts/verify_deploy_tooling.sh
DRAMAFLOW_EVIDENCE_DIR="${EVIDENCE_DIR}" sh deployments/scripts/verify_github_environment_setup.sh
DRAMAFLOW_EVIDENCE_DIR="${EVIDENCE_DIR}" sh deployments/scripts/verify_secret_readiness.sh

node tests/release/verify_production_gate.mjs
