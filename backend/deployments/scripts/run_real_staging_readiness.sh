#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "${ROOT_DIR}"

export DRAMAFLOW_TARGET_ENV="${DRAMAFLOW_TARGET_ENV:-staging}"
export DRAMAFLOW_EVIDENCE_DIR="${DRAMAFLOW_EVIDENCE_DIR:-${ROOT_DIR}/release-evidence}"

sh deployments/scripts/intake_platform_inputs.sh
sh deployments/scripts/verify_deploy_tooling.sh
sh deployments/scripts/reconcile_environment_blockers.sh

echo "[run-real-staging-readiness] pass"
