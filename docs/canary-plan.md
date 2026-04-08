# Canary Plan

## Why Canary Now

DramaFlow is past the point where local green tests are enough. Billing, entitlement, playback, and admin dangerous actions are high-impact mutations; if they regress at 100% rollout, rollback time becomes expensive. Canary is the cheapest way to buy signal before blast radius.

## Services That Must Support Canary First

- `admin-service`
- `playback-service`
- `billing-service`
- `entitlement-service`
- `feed-service`

## Canary Observation Window

- initial slice: 5% to 10%
- minimum watch time: 15 minutes
- review every 5 minutes
- do not advance if any critical release-watch metric regresses beyond threshold

## Advance Gates

- `http_request_errors_total` stable
- P95 latency within documented threshold
- `billing_purchase_sync_error_total` no abnormal increase
- `playback_session_create_error_total` no abnormal increase
- `admin_login_error_total` stable
- no unexplained shift in `playback_access_none/full/preview` ratio

## Immediate Rollback Triggers

- playback session create error spike
- billing sync error spike
- entitlement revoke/recompute failures
- 5xx surge above alert threshold
- migration side effects on reads or writes

## Operator Commands

```sh
sh backend/deployments/scripts/canary_check.sh
sh backend/deployments/scripts/release_watch.sh
sh backend/deployments/scripts/rollback.sh <service> <migration>
```

## Production Release Rule

No production rollout may advance beyond canary unless:

1. staging rehearsal on the same candidate commit has passed,
2. canary watch window is green,
3. rollback target is named and reachable,
4. oncall owner confirms active coverage.
