# Billion-Traffic Code Hardening Plan (Local-Code Scope)

## Current Position
- Current codebase status: `mid-scale production skeleton + strong release governance`
- Current local conclusion: `not yet billion-traffic production grade`
- This plan only covers code hardening that can be executed locally before real staging/prod evidence is available.

## P0 (Do First)

1. **Global request guards (done in this round)**
- Added shared middleware for:
  - request body size limit
  - in-flight concurrency gate
  - per-request handler timeout context
- Applied to all 8 core services (`auth/content/feed/progress/playback/entitlement/billing/admin`).
- Goal: prevent single-node overload and unbounded request memory/latency blowups.

2. **Hot-path read caching with anti-breakdown**
- Add two-level cache strategy for feed/detail/playback descriptor paths:
  - short TTL local memory cache
  - Redis cache with jittered TTL and soft-expire
- Add singleflight/request-coalescing on cache-miss hot keys.

3. **Strict idempotency on billing + entitlement mutation paths**
- Enforce idempotency keys on purchase sync / grant / revoke / recompute.
- Persist dedupe keys with bounded retention and conflict-safe upsert.

4. **Service-to-service timeout budget split**
- Define hop timeout budgets (caller budget > callee budget).
- Fail fast with explicit upstream timeout code mapping and retry policy caps.

5. **Backpressure + queue isolation for async compensation**
- Isolate high-cost compensation jobs from request threads.
- Add bounded queue + drop/retry policy with metrics for saturation.

## P1 (Do Next)

6. **DB access hardening**
- Query timeout + context propagation enforced in all repositories.
- Add slow-query classification and index hint backlog evidence.
- Move heavy writes to async/batch where correctness allows.

7. **Read/write traffic split readiness**
- Introduce repository interfaces ready for read replica routing.
- Keep feature-flag switch for replica fallback to primary.

8. **Circuit breaking for critical downstreams**
- Add breaker around entitlement/billing/playback cross-service calls.
- Include half-open probe strategy and fallback error shaping.

9. **Contract drift protection**
- Add schema/DTO compatibility tests for null/empty/unknown fields.
- Block merges on contract break in mobile-critical DTOs.

10. **Load-test feedback loop into code**
- Every load/soak run must produce:
  - top latency contributors
  - top error contributors
  - exact code-level remediation list
- Store in release evidence and tie to a fix PR before next run.

## Execution Order (Minimum-Risk)
1. Request guards (already landed)
2. Idempotency + timeout budgets
3. Hot key anti-breakdown
4. Queue isolation + DB timeout enforcement
5. Circuit breaker + contract gates
6. Load feedback loop

## Done in This Round
- Implemented and wired global request guards across all service routers.
- Added environment controls:
  - `DRAMAFLOW_HTTP_HANDLER_TIMEOUT` (default `8s`)
  - `DRAMAFLOW_HTTP_MAX_BODY_BYTES` (default `1048576`)
  - `DRAMAFLOW_HTTP_MAX_INFLIGHT` (default `1000`)

