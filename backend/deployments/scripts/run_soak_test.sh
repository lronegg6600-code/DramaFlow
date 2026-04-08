#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "${ROOT_DIR}"

RUN_KIND="${DRAMAFLOW_REAL_RUN:-0}"
EVIDENCE_DIR="${DRAMAFLOW_EVIDENCE_DIR:-${ROOT_DIR}/release-evidence/soak}"

mkdir -p "${EVIDENCE_DIR}"
echo "[soak-test] real_run=${RUN_KIND}" | tee "${EVIDENCE_DIR}/soak-test.log"

if command -v k6 >/dev/null 2>&1; then
  echo "[soak-test] running k6 soak"
  k6 run loadtest/k6/feed_home.js
  exit 0
fi

if docker image inspect grafana/k6 >/dev/null 2>&1; then
  echo "[soak-test] running dockerized k6 soak"
  docker run --rm --network host -v "${ROOT_DIR}:/workspace" -w /workspace/backend grafana/k6 run loadtest/k6/feed_home.js
  exit 0
fi

echo "[soak-test] k6 unavailable; running substitute soak evidence"
node tests/release/soak_substitute.mjs | tee "${EVIDENCE_DIR}/soak-substitute.json"
