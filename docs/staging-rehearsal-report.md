# Staging Rehearsal Report

## Goal

Verify whether the current DramaFlow release candidate can complete a real staging deployment rehearsal, and document exactly where the execution succeeded versus where it stopped due to missing real-environment prerequisites.

## Execution Environment

- Date: `2026-04-08`
- Operator: Codex local execution
- Workspace: `Z:\Projects\DramaFlow`
- Runtime mode attempted: `real staging`
- Runtime mode achieved: `local-compose-surrogate only`
- Host: Windows + Docker Desktop
- Hard limits observed during Phase 9:
  - no `.git` metadata
  - `kubectl` installed but `current-context` is not set
  - no `helm`
  - no `gh`
  - no visible staging / production credentials in environment variables

## Candidate Input

- Candidate commit SHA: `unavailable`
- Candidate tag: `unavailable`
- Candidate branch: `unavailable`
- Reason: workspace does not contain `.git`; the exact RC commit cannot be proven from the local checkout.
- Services under rehearsal:
  - `admin-service`
  - `auth-service`
  - `content-service`
  - `feed-service`
  - `progress-service`
  - `playback-service`
  - `entitlement-service`
  - `billing-service`
  - `admin-web`
  - `postgres`
  - `redis`
  - `otel-collector`
  - `prometheus`

## Executed Actions

### 1. Real staging prerequisite check

Executed:

- `git rev-parse --show-toplevel`
- `kubectl config current-context`
- `kubectl config get-contexts`
- environment variable scan for staging / production release credentials

Observed result:

- git root: unavailable
- kubernetes current context: unavailable
- kubernetes contexts: none configured
- visible deploy credentials: none found in current environment

Decision at this point:

- real staging rehearsal could not proceed
- execution downgraded to local surrogate only

### 2. Pre-deploy / safety checks

Executed locally:

- `docker compose -f backend/deployments/docker-compose/docker-compose.yml config`
- `node backend/tests/contract/check_openapi.mjs`
- migration pair check equivalent to `migration_check.sh`

Observed result:

- compose config: pass
- OpenAPI contract check: pass
- migration pair check: pass, output `"[migration-check] ok"`

Not executed as-is:

- `backend/deployments/scripts/pre_deploy_check.sh`
- reason: current Windows host has no local `sh` runtime; the shell script itself is valid for Linux runners and CI, but this host can only execute the equivalent component checks above.

### 3. Deploy / upgrade surrogate

Executed:

- `docker compose -f backend/deployments/docker-compose/docker-compose.yml up --build -d admin-service`

Observed result:

- `admin-service` rebuilt and container returned to `Up` state.
- Existing stack stayed healthy enough to continue smoke and integration flows.

### 4. Post-deploy verify

Executed:

- `node backend/tests/release/verify_staging_gate.mjs`

Observed result:

- output `"[staging-gate] pass"`

### 5. Release smoke

Executed:

- `DRAMAFLOW_RUN_INTEGRATION=1 node backend/tests/release/smoke_release.mjs`

Observed result:

- output `"[release-smoke] release smoke passed"`

### 6. Critical release flows

Executed:

- `billing_entitlement_playback_flow.mjs`
- `entitlement_revoke_access_downgrade.mjs`
- `admin_audit_flow.mjs`
- `feed_publish_visibility.mjs`

Observed results:

- `billing -> entitlement -> playback`: pass
- `revoke -> playback downgrade`: pass
- `admin dangerous action -> audit`: pass
- `feed publish -> downstream visibility`: pass

### 7. Dashboard / alert / logs check

Executed:

- Prometheus watch queries via local API
- `docker ps` to confirm service state
- `verify_canary_gate.mjs` for alert-free release watch evaluation

Observed result summary:

- firing alerts: none
- `dramaflow_playback_session_create_error_total`: `0`
- `dramaflow_http_errors_total`: `3` historical errors present, but no fresh 1-minute error growth during gate execution
- service containers remained `Up`; `postgres` and `redis` remained `healthy`

## Evidence Summary

- `admin` vitest: `8` files, `24` tests passed
- release smoke: passed
- staging gate: passed
- integration flows: `4/4` passed
- canary gate: passed in local surrogate mode
- rollback gate: passed in local surrogate mode

## Failed / Blocked Items

1. No real staging cluster context was configured.
   - Impact: no real deploy / upgrade / rollout command could be executed against staging.
2. No `.git` metadata exists in the workspace.
   - Impact: candidate SHA cannot be proven in the evidence pack.
3. No release credentials or artifact identity variables were present.
   - Impact: image tag, digest, workflow run, and approver context cannot be attached as real RC evidence.
4. Shell-based release scripts were not executed directly on the local Windows host.
   - Impact: local proof comes from equivalent component commands plus Node gates; Linux shell execution still needs CI or staging runner evidence.

## Conclusion

- Local surrogate staging rehearsal: `pass`
- Real staging rehearsal: `not executed`
- Real staging rehearsal sign-off quality: `blocked`

## Follow-up Actions

1. Provide a real repository checkout with `.git` metadata.
2. Provide staging cluster context, namespace, and deploy credentials.
3. Attach environment name, image tags, commit SHA, and operator sign-off.
4. Re-run `smoke_release.mjs` and the `4` critical integration flows in staging.
5. Capture Grafana board screenshots or exported panels during the 15-minute watch window.

## Phase 11 Update

- real-input intake was executed again
- `staging-unblock-summary.json` still reports `6` blocker categories remaining
- result: real staging rehearsal was **not triggered** in Phase 11
