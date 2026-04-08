# Load Test Guide

## Why Phase 7 loads these endpoints first

- `feed/home` is user-facing read hot path
- `playback/sessions` is revenue-critical authorization path
- `billing sync` is subscription truth source
- `admin dramas` and `feed publish` cover operator workflows

## Scenarios

- baseline: release regression gate
- spike: sudden burst and saturation behavior
- soak: placeholder for long-running stability validation

## Release usage rule

- baseline run is mandatory before a release candidate is promoted from staging.
- spike run is mandatory when billing, entitlement, or playback code changes.
- soak remains recommended before scale-up, but does not replace release smoke or canary watch.

## Thresholds

- `/v1/feed/home` P95 <= 300ms
- `/v1/playback/sessions` P95 <= 400ms
- `/v1/billing/google-play/purchases:sync` P95 <= 800ms
- admin list / publish P95 <= 500ms
- failure rate <= 1%

## Phase 8 Note

On `2026-04-08`, a local substitute soak run was executed through `backend/tests/release/soak_substitute.mjs` for `120s`.

- result: pass
- failures: `0`
- evidence class: `substitute only`

This is acceptable as rehearsal evidence for staging preparation, but it does **not** unlock production by itself.

## Phase 9 Reality Check

Production-grade soak is still blocked in the current environment because:

- there is no real staging/pre-production cluster context
- there is no artifact-tagged candidate deployment identity
- there is no long-running shared environment to observe resource drift

Any production decision without closing those gaps must remain `no-go` or go through a formal waiver.
