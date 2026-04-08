# Canary Drill Report

## Goal

Exercise a real canary drill if the environment allows it, and otherwise record exactly why only a surrogate canary was possible.

## Execution Environment

- Date: `2026-04-08`
- Requested mode: `real staging canary`
- Achieved mode: `local-compose-surrogate`
- Selected service: `admin-service`
- Reason for selection: low blast radius compared with billing/playback, while still exercising admin login, dramas list, audit, and operator-facing workflows.

## Candidate Input

- Candidate commit SHA: `unavailable`
- Blocking reason: local workspace has no `.git` metadata

## Real Environment Attempt

Executed prerequisite checks:

- git metadata lookup
- kubernetes current-context lookup
- staging credential/env scan

Observed result:

- no commit SHA available from local workspace
- no kubernetes context configured
- no visible staging rollout credentials

Decision:

- true canary could not run
- drill downgraded to local surrogate

## Executed Actions

1. Rebuilt and re-applied `admin-service`
   - command equivalent:
     - `docker compose -f backend/deployments/docker-compose/docker-compose.yml up --build -d admin-service`
2. Ran canary watch gate
   - `node backend/tests/release/verify_canary_gate.mjs`
3. Re-ran release smoke after the rollout surrogate
   - `DRAMAFLOW_RUN_INTEGRATION=1 node backend/tests/release/smoke_release.mjs`

## Observed Gate Values

- `httpErrors1m`: `0`
- `billingSyncErrors1m`: `0`
- `playbackCreateErrors1m`: `0`
- firing alerts: none observed during the gate query window

Historical counter note:

- `dramaflow_http_errors_total` was non-zero historically in Prometheus, but the canary gate is based on recent 1-minute growth, which was `0` during this drill.

## Result

- Canary surrogate: `pass`
- Real canary: `not executed`
- Release watch decision on surrogate evidence: `continue`

## Why This Is Not Yet Production-Grade Canary Evidence

1. Only one local instance existed; there was no percentage traffic split.
2. There was no separate baseline version versus canary version running side-by-side.
3. No staging/prod ingress, service mesh, or load balancer policy was involved.
4. No real deploy controller context was available at execution time.

## Conclusion

- Canary logic and stop conditions are executable and gave a stable `pass` result.
- This is enough to support `staging-ready with surrogate canary evidence`.
- This is not enough to claim `production canary validated`.

## Follow-up Actions

1. Run the same gate in staging with at least one old replica and one candidate replica.
2. Observe watchlist metrics for a full 15-minute window.
3. Record explicit promote / hold / rollback operator decision with approver names.
