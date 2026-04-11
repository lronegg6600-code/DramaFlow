#!/usr/bin/env bash
set -euo pipefail
node "$(cd "$(dirname "$0")/../../.." && pwd)/tools/summarize_mobile_integration_resume.mjs"
