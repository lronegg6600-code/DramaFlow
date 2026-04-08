#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "${ROOT_DIR}"

TARGET_ENV="${DRAMAFLOW_TARGET_ENV:-staging}"
export DRAMAFLOW_EVIDENCE_DIR="${DRAMAFLOW_EVIDENCE_DIR:-${ROOT_DIR}/release-evidence}"

mkdir -p "${DRAMAFLOW_EVIDENCE_DIR}"

echo "[intake-platform-inputs] target_env=${TARGET_ENV}"
node tests/release/verify_real_repo_identity.mjs
node tests/release/verify_real_artifacts.mjs
node tests/release/verify_real_cluster_access.mjs
node tests/release/verify_real_github_envs.mjs
node tests/release/verify_real_secret_readiness.mjs
