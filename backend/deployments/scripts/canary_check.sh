#!/bin/sh
set -eu

WINDOW_MINUTES="${WINDOW_MINUTES:-15}"
echo "[canary] inspect for at least ${WINDOW_MINUTES} minutes before advancing rollout"
echo "[canary] required watch metrics:"
echo "  - admin_login_error_total"
echo "  - billing_purchase_sync_error_total"
echo "  - entitlement_revoke_total / entitlement_recompute_total"
echo "  - playback_session_create_error_total"
echo "  - dramaflow_http_errors_total"
echo "  - dramaflow_http_request_duration_seconds"
echo "[canary] rollback immediately if any critical alert fires and persists past 5 minutes"
