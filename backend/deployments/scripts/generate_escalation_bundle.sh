#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
EVIDENCE_DIR="${DRAMAFLOW_EVIDENCE_DIR:-$ROOT_DIR/release-evidence}"
OUT_FILE="$EVIDENCE_DIR/escalation-bundle.md"

mkdir -p "$EVIDENCE_DIR"

cat > "$OUT_FILE" <<EOF
# Escalation Bundle

- Escalation template: platform-intake/escalation/blocker-escalation-template.md
- Platform request template: platform-intake/escalation/platform-request-template.md
- Repo admin request template: platform-intake/escalation/repo-admin-request-template.md
- Ops request template: platform-intake/escalation/ops-request-template.md
- Release manager request template: platform-intake/escalation/release-manager-request-template.md
- Current blocker board: docs/unblock-status-board.md
- Remaining blockers: docs/remaining-environment-blockers.md
EOF

echo "[generate_escalation_bundle] wrote $OUT_FILE"
