# Rollback Drill Report

## Goal

Verify that a release operator can recover service health and critical smoke checks after rollback, and document whether the rollback was truly environment-real or only a surrogate.

## Execution Environment

- Date: `2026-04-08`
- Requested mode: `real staging rollback`
- Achieved mode: `local-compose-surrogate`
- Selected service: `admin-service`

## Candidate Input

- Candidate commit SHA: `unavailable`
- Blocking reason: local workspace has no `.git` metadata

## Real Environment Attempt

Executed prerequisite checks:

- git metadata lookup
- kubernetes current-context lookup
- staging credential/env scan

Observed result:

- no provable candidate SHA
- no rollback-capable cluster context
- no visible previous release artifact information

Decision:

- true rollback drill could not run
- drill downgraded to local surrogate

## Executed Actions

1. Simulated service-level rollback surrogate by restarting `admin-service`
   - command equivalent:
     - `docker compose -f backend/deployments/docker-compose/docker-compose.yml restart admin-service`
2. Verified rollback gate
   - `node backend/tests/release/verify_rollback_gate.mjs`
3. Re-ran release smoke
   - `DRAMAFLOW_RUN_INTEGRATION=1 node backend/tests/release/smoke_release.mjs`

## Observed Results

- rollback gate output: `"[rollback-gate] pass"`
- release smoke output: `"[release-smoke] release smoke passed"`
- `admin-service` returned to healthy/available state after restart

## What This Drill Proves

- Operator can use the rollback verification path and regain service readiness in the current local stack.
- The rollback gate covers:
  - key service health checks
  - guest entitlement access check
  - playback session create
  - admin login
  - admin dramas list

## What This Drill Does Not Prove

1. This was not an image-tag rollback to a previous immutable release.
2. This did not exercise schema rollback.
3. This did not involve a real staging or production deployment controller.
4. No previous release artifact identity was available.

## Conclusion

- Rollback surrogate: `pass`
- Real rollback drill: `not executed`
- Real production rollback evidence: `still missing`

## Follow-up Actions

1. Run one true image-version rollback in staging.
2. Record previous image tag, target image tag, and rollback trigger condition.
3. Capture post-rollback smoke outputs and watchlist metrics.
