# Test Strategy

## Layers

- admin component + page + flow tests via Vitest
- backend service tests inside each service module
- contract checks against OpenAPI specs
- integration smoke tests for deployed local stack
- release smoke tests for go/no-go rehearsal
- k6 load tests for release gate

## Phase 7 focus

- admin login and dangerous mutations
- billing purchase sync / RTDN idempotency
- entitlement recompute / revoke / grant-temp
- playback create / heartbeat / refresh / complete

## Phase 7.5 release-focused additions

- purchase sync -> entitlement grant -> playback access
- entitlement revoke -> playback downgrade
- admin dangerous action -> audit log written
- feed publish -> downstream visibility
- release smoke for staging/prod candidate gate

## Execution

- admin: `corepack pnpm test`
- backend unit: `make -C backend test`
- backend contract: `make -C backend contract-test`
- backend integration: `DRAMAFLOW_RUN_INTEGRATION=1 make -C backend integration-test`
- backend release smoke: `DRAMAFLOW_RUN_INTEGRATION=1 make -C backend release-smoke`

## Phase 8 Rehearsal Result

- admin tests: `8` files / `24` tests passed
- release smoke: passed
- billing -> entitlement -> playback: passed
- entitlement revoke -> playback downgrade: passed
- admin dangerous action -> audit log: passed
- feed publish -> downstream visibility: passed
- current remaining gap: real staging/prod evidence, not local code-path coverage
