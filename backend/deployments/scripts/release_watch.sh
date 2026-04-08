#!/bin/sh
set -eu

PROM_URL="${PROM_URL:-http://localhost:9090}"
GRAFANA_URL="${GRAFANA_URL:-http://localhost:3001}"

echo "[release-watch] prometheus=${PROM_URL}"
echo "[release-watch] grafana=${GRAFANA_URL}"
echo "[release-watch] watchlist:"
echo "  - admin_login_error_total"
echo "  - billing_purchase_sync_error_total"
echo "  - billing_rtdn_duplicate_total"
echo "  - entitlement_grant_total"
echo "  - entitlement_revoke_total"
echo "  - entitlement_recompute_total"
echo "  - playback_session_create_error_total"
echo "  - playback_access_none_total / preview_total / full_total"
echo "  - dramaflow_http_request_duration_seconds"
echo "  - dramaflow_http_errors_total"
