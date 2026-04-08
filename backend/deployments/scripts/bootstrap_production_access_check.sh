#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "${ROOT_DIR}"

export DRAMAFLOW_TARGET_ENV=production
export DRAMAFLOW_KUBE_NAMESPACE="${DRAMAFLOW_KUBE_NAMESPACE:-dramaflow-production}"
export DRAMAFLOW_EVIDENCE_DIR="${DRAMAFLOW_EVIDENCE_DIR:-${ROOT_DIR}/release-evidence/production-bootstrap}"
mkdir -p "${DRAMAFLOW_EVIDENCE_DIR}"

sh deployments/scripts/verify_repo_identity.sh
sh deployments/scripts/verify_artifact_identity.sh
sh deployments/scripts/verify_cluster_access.sh
sh deployments/scripts/verify_deploy_tooling.sh
sh deployments/scripts/verify_github_environment_setup.sh
sh deployments/scripts/verify_secret_readiness.sh

echo "[bootstrap-production-access-check] pass"
