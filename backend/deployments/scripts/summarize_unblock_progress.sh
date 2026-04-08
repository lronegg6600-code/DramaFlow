#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
BOARD_FILE="${DRAMAFLOW_EVIDENCE_DIR:-$ROOT_DIR/release-evidence}/unblock-status-board.json"

python - <<'PY' "$BOARD_FILE"
import json
import sys

board_file = sys.argv[1]
with open(board_file, "r", encoding="utf-8") as fh:
    payload = json.load(fh)

counts = payload["blockerCounts"]
print("[summarize_unblock_progress]")
print(f"ready_for_platform_handoff={str(payload['readyForPlatformHandoffExecution']).lower()}")
print(f"ready_for_real_staging={str(payload['readyForRealStagingExecution']).lower()}")
print(f"total={counts['total']}")
print(f"not_received={counts['not_received']}")
print(f"received_but_invalid={counts['received_but_invalid']}")
print(f"received_and_verified={counts['received_and_verified']}")
print(f"next_action={payload['nextAction']}")
PY
