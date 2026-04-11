#!/bin/sh
set -eu
ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)"
cd "${ROOT_DIR}"
node tools/summarize_real_external_url_burndown.mjs
