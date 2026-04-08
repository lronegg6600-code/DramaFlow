#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$ROOT_DIR"

node backend/tests/release/validate_repo_input_package.mjs || true
node backend/tests/release/validate_artifact_input_package.mjs || true
node backend/tests/release/validate_cluster_input_package.mjs || true
node backend/tests/release/validate_github_env_input_package.mjs || true
node backend/tests/release/validate_secret_input_package.mjs || true
node backend/tests/release/validate_deploy_tooling_input_package.mjs || true

if node backend/tests/release/sync_blocker_status_from_inputs.mjs; then
  echo "[validate_received_inputs] pass"
else
  echo "[validate_received_inputs] blocker"
  exit 1
fi
