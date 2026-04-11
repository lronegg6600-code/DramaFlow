#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
node "$ROOT/tools/audit_workspace_layout.mjs" || true
node "$ROOT/tools/find_source_tree_candidates.mjs" || true
node "$ROOT/tools/relink_workspace_if_sources_found.mjs" || true
node "$ROOT/tools/bootstrap_recovered_workspace.mjs" || true
node "$ROOT/tools/rerun_mobile_readiness_if_recovered.mjs" || true
