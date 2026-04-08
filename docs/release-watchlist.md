# Release Watchlist

## First 15 Minutes Matter Most

Do not spread attention across every chart. During release, watch the metrics that tell you whether users can log in, whether purchases turn into rights, and whether premium playback still works.

## Release-Day Watch Order

1. `admin_login_error_total`
2. `billing_purchase_sync_error_total`
3. `billing_rtdn_duplicate_total`
4. `entitlement_grant_total`
5. `entitlement_revoke_total`
6. `entitlement_recompute_total`
7. `playback_session_create_error_total`
8. `playback_access_none_total`
9. `playback_access_preview_total`
10. `playback_access_full_total`
11. `dramaflow_http_request_duration_seconds`
12. `dramaflow_http_errors_total`

## What To Look For

- Admin login failures rising after deploy means operator workflows are already degraded.
- Billing sync errors rising means revenue truth is at risk; stop rollout first, diagnose second.
- Playback `none/full/preview` ratio shifting unexpectedly is often the earliest signal that entitlement convergence broke.
- HTTP latency rising without error growth often means a dependency, migration, or cache path is degraded before users hard-fail.

## First Actions By Symptom

- admin login error spike: pause rollout, verify admin-service health, inspect auth cookie/session behavior.
- billing sync error spike: stop rollout, inspect billing-service logs by `purchase_token`, confirm entitlement downstream writes.
- playback session create error spike: stop rollout, inspect playback-service and entitlement-service traces, verify premium path.
- entitlement recompute or revoke anomalies: freeze admin dangerous operations until audit and entitlement counts stabilize.

## Phase 8 Local Rehearsal Snapshot - 2026-04-08

- `playback_session_create_error_total`: `0`
- `http_request_errors_total`: historical value present, but `1m` growth during canary gate was `0`
- firing alerts during gate query: none
- note: several counters were not emitted in the short local rehearsal window and therefore appeared as `null`; treat that as an environment-observation gap, not as production proof

## Phase 9 Real Environment Attempt

- real staging/prod watch window was **not** executed
- reason:
  - no `.git` commit identity
  - no kubernetes current-context
  - no visible release credentials
- decision impact:
  - watchlist remains valid
  - production sign-off cannot rely on local-only watch evidence
