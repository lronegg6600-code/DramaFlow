#!/usr/bin/env bash
set -euo pipefail
node "$(cd "$(dirname "$0")/../../.." && pwd)/tools/summarize_staging_url_burndown.mjs"
