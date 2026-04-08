#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "${ROOT_DIR}"

export DRAMAFLOW_TARGET_ENV="${DRAMAFLOW_TARGET_ENV:-staging}"
export DRAMAFLOW_EVIDENCE_DIR="${DRAMAFLOW_EVIDENCE_DIR:-${ROOT_DIR}/release-evidence}"

sh deployments/scripts/run_real_staging_readiness.sh

export DRAMAFLOW_REAL_RUN=1
export DRAMAFLOW_CONTROL_PLANE="${DRAMAFLOW_CONTROL_PLANE:-kubernetes}"
sh deployments/scripts/run_staging_rehearsal.sh
