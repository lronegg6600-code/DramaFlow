#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "${ROOT_DIR}"

export DRAMAFLOW_EVIDENCE_DIR="${DRAMAFLOW_EVIDENCE_DIR:-${ROOT_DIR}/release-evidence}"
node tests/release/verify_real_github_envs.mjs
