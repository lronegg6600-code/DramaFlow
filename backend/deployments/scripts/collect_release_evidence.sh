#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "${ROOT_DIR}/.."
EVIDENCE_DIR="${DRAMAFLOW_EVIDENCE_DIR:-${ROOT_DIR}/release-evidence}"

mkdir -p "${EVIDENCE_DIR}"

echo "[release-evidence] collecting metadata"
DRAMAFLOW_EVIDENCE_DIR="${EVIDENCE_DIR}" node backend/tests/release/collect_release_metadata.mjs > "${EVIDENCE_DIR}/release-metadata.path"

echo "[release-evidence] required docs"
for file in \
  docs/staging-rehearsal-report.md \
  docs/canary-drill-report.md \
  docs/rollback-drill-report.md \
  docs/soak-test-report.md \
  docs/release-evidence-pack.md \
  docs/production-go-no-go.md \
  docs/release-blockers-final.md \
  docs/release-candidate-manifest.md
do
  test -f "${file}"
  echo "  - ${file}"
done

cat > "${EVIDENCE_DIR}/release-evidence-summary.txt" <<EOF
release_evidence_collected_at=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
evidence_dir=${EVIDENCE_DIR}
required_docs=ok
EOF

echo "[release-evidence] ok"
