# Rollback Guide

## Phase 9 Reality Check

Do not mark rollback as production-ready unless all of the following exist at execution time:

- provable RC commit SHA
- previous immutable release artifact or image tag
- real deploy control-plane access
- post-rollback smoke outputs
- rollback drill report signed by operator/reviewer

## Trigger rollback when

- billing / entitlement / playback main path fails after deploy
- migration caused read / write regression
- 5xx or latency alerts stay red after short mitigation

## Steps

1. Freeze deploys and announce incident owner.
2. Decide whether code rollback only or code + migration rollback.
3. Run `sh backend/deployments/scripts/rollback.sh <service> <migration>`
4. Run `sh backend/deployments/scripts/rollback_verify.sh`
5. Verify health endpoints and dashboards.
6. Re-run smoke path: admin login, feed, purchase sync, playback session create.

## Service-specific first rollback targets

- billing / entitlement / playback: rollback immediately if premium access path diverges
- admin-service: rollback if dangerous operations lose audit or RBAC
- feed-service: rollback if publish is accepted but downstream feed stays stale
