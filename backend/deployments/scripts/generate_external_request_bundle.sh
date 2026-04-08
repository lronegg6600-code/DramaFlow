#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
EVIDENCE_DIR="${DRAMAFLOW_EVIDENCE_DIR:-$ROOT_DIR/release-evidence}"
OUT_FILE="$EVIDENCE_DIR/external-request-bundle.md"

mkdir -p "$EVIDENCE_DIR"

cat > "$OUT_FILE" <<EOF
# External Request Bundle

- Platform request: docs/platform-request-bundle.md
- Repo admin request: docs/repo-admin-request-bundle.md
- Ops request: docs/ops-request-bundle.md
- Release manager request: docs/release-manager-request-bundle.md
- Escalation policy: docs/blocker-escalation-policy.md
- Owner matrix: docs/platform-blocker-owner-matrix.md
- Intake root: platform-intake/received/
- Progress board: docs/unblock-status-board.md
EOF

echo "[generate_external_request_bundle] wrote $OUT_FILE"
