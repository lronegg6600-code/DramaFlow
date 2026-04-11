#!/usr/bin/env bash
set -euo pipefail
node "$(cd "$(dirname "$0")/../../.." && pwd)/tools/rerun_mobile_integration_with_external_urls.mjs"
