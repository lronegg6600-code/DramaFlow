#!/bin/sh
set -eu
ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)"
cd "${ROOT_DIR}"
node tools/dispatch_real_external_url_escalation.mjs
