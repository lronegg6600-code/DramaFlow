# Trace Walkthrough: Purchase Sync -> Entitlement -> Playback Access

## Goal

This walkthrough explains what a healthy trace should look like when a purchase enters the system and eventually results in full playback access.

## Expected Span Order

1. `billing-service` receives `POST /v1/billing/google-play/purchases:sync`
2. billing validation / idempotency span
3. billing persistence span
4. service-to-service call into entitlement mutation path
5. `entitlement-service` grant or recompute span
6. entitlement persistence span
7. later, `playback-service` receives `POST /v1/playback/sessions`
8. playback access check calls entitlement lookup
9. playback descriptor creation span

## Required Trace Correlation

- `trace_id` must be shared across service hops where synchronous calls exist
- `request_id` must be present on ingress and attached to service logs
- `purchase_token` should be visible in billing logs
- `user_id` must be visible in entitlement and playback logs

## What Healthy Looks Like

- Billing returns accepted without duplicate mutation for the same purchase.
- Entitlement writes exactly one active grant for the purchase state.
- Playback access resolves to `full` for a premium episode after grant.

## What Broken Looks Like

- Billing accepts but entitlement never mutates.
- Duplicate RTDN causes repeated entitlement mutations.
- Playback session create stays on `preview` or `none` after a successful premium purchase.

## Immediate Triage

1. Check billing logs filtered by `purchase_token`.
2. Follow trace into entitlement mutation.
3. Compare entitlement state against playback access result.
4. If mismatch remains, stop rollout and prefer rollback before retry storms start.
