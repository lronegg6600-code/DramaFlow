#!/bin/sh
set -eu
ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)"
cd "${ROOT_DIR}"
node tools/close_invalid_candidate_cycle.mjs
