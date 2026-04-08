# Observability Guide

## Standard log fields

- `ts`
- `level`
- `service`
- `env`
- `trace_id`
- `request_id`
- `user_id`
- `admin_user_id`
- `purchase_token`
- `session_id`
- `episode_id`
- `drama_id`
- `action`
- `error_code`
- `msg`

## Key metrics to watch first

- `dramaflow_http_request_duration_seconds`
- `dramaflow_http_errors_total`
- `admin_login_error_total`
- `billing_purchase_sync_error_total`
- `entitlement_grant_total`
- `dramaflow_playback_session_create_error_total`

## Dashboards

- `platform-overview.json`
- `admin-operations.json`
- `billing-entitlement.json`
- `playback-health.json`
- `release-gate.json`

## Tracing scope

- HTTP server spans
- service-to-service client calls
- playback and billing main path traces
- admin dangerous mutation to audit path traces
