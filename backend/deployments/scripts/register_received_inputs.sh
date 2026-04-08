#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
INTAKE_DIR="${DRAMAFLOW_INTAKE_DIR:-$ROOT_DIR/platform-intake/received}"
EVIDENCE_DIR="${DRAMAFLOW_EVIDENCE_DIR:-$ROOT_DIR/release-evidence}"
OUT_FILE="$EVIDENCE_DIR/platform-input-intake-register.json"

mkdir -p "$EVIDENCE_DIR"

python - <<'PY' "$INTAKE_DIR" "$OUT_FILE"
import json
import os
import sys

intake_dir = sys.argv[1]
out_file = sys.argv[2]
categories = [
    "repo-identity",
    "artifact-identity",
    "cluster-access",
    "github-environments",
    "secrets",
    "deploy-tooling",
]

payload = {"generatedAt": __import__("datetime").datetime.utcnow().isoformat() + "Z", "categories": []}
for category in categories:
    category_dir = os.path.join(intake_dir, category)
    files = []
    if os.path.isdir(category_dir):
        for name in sorted(os.listdir(category_dir)):
            if name.endswith((".yaml", ".yml", ".json")):
                files.append(name)
    payload["categories"].append({
        "category": category,
        "receivedCount": len(files),
        "files": files,
        "status": "received" if files else "not_received"
    })

with open(out_file, "w", encoding="utf-8") as fh:
    json.dump(payload, fh, indent=2)
    fh.write("\n")

if any(entry["receivedCount"] for entry in payload["categories"]):
    print("[register_received_inputs] registered")
else:
    print("[register_received_inputs] no_input_files_detected")
PY
