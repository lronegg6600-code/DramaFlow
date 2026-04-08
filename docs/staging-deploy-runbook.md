# Staging Deploy Runbook

## Goal

Use this runbook when promoting a DramaFlow release candidate into staging. The point is not only to deploy, but to prove the candidate is observable, smoke-tested, and rollbackable.

## Inputs

- release candidate commit SHA
- service list impacted by the release
- migration list
- staged environment files:
  - [admin/.env.staging.example](/Z:/Projects/DramaFlow/admin/.env.staging.example)
  - [backend/.env.staging.example](/Z:/Projects/DramaFlow/backend/.env.staging.example)
  - [backend/deployments/docker-compose/.env.observability.staging.example](/Z:/Projects/DramaFlow/backend/deployments/docker-compose/.env.observability.staging.example)
  - [android/local.staging.example.properties](/Z:/Projects/DramaFlow/android/local.staging.example.properties)

## Step 1: Pre-deploy

Run:

```sh
sh backend/deployments/scripts/pre_deploy_check.sh
```

Confirm:

- CI is green on the candidate commit.
- migration rollback pairs exist.
- release defect matrix has no open P0 or blocking P1.
- backup/snapshot owner is named.

## Step 2: Deploy staging

Run:

```sh
sh backend/deployments/scripts/staging_deploy.sh
```

## Step 3: Run staging smoke

Run:

```sh
DRAMAFLOW_RUN_INTEGRATION=1 node backend/tests/release/smoke_release.mjs
DRAMAFLOW_RUN_INTEGRATION=1 node backend/tests/integration/billing_entitlement_playback_flow.mjs
DRAMAFLOW_RUN_INTEGRATION=1 node backend/tests/integration/entitlement_revoke_access_downgrade.mjs
DRAMAFLOW_RUN_INTEGRATION=1 node backend/tests/integration/admin_audit_flow.mjs
DRAMAFLOW_RUN_INTEGRATION=1 node backend/tests/integration/feed_publish_visibility.mjs
```

## Step 4: Release watch

Run:

```sh
sh backend/deployments/scripts/release_watch.sh
```

Watch for 15 minutes minimum:

- admin login errors
- billing sync errors
- entitlement recompute / revoke anomalies
- playback session create errors
- HTTP error rate and latency

## Step 5: Staging success criteria

Staging deploy is successful only if all conditions are true:

- health checks all pass
- release smoke passes
- all four critical rehearsal flows pass
- Prometheus, Alertmanager, and Grafana are reachable
- no critical alert stays red for more than 5 minutes
- rollback path is confirmed for affected services and migrations

## Step 6: If staging fails

Stop immediately when:

- billing or playback chain fails
- feed publish is not visible downstream
- admin dangerous action is missing audit evidence
- rollback path is unclear

Then:

```sh
sh backend/deployments/scripts/rollback.sh <service> <migration>
sh backend/deployments/scripts/rollback_verify.sh
```
