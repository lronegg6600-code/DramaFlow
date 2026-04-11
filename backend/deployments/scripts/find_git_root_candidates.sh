#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
node "$ROOT/tools/audit_workspace_layout.mjs" >/dev/null || true
cat "$ROOT/release-evidence/git-root-candidates.json"
