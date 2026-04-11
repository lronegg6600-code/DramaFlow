#!/usr/bin/env bash
set -euo pipefail
node "$(cd "$(dirname "$0")/../../.." && pwd)/tools/export_android_staging_env.mjs"
