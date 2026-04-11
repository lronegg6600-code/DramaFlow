#!/usr/bin/env bash
set -euo pipefail
node "$(cd "$(dirname "$0")/../../.." && pwd)/tools/validate_external_staging_urls.mjs"
