# Oncall Runbook

## First 10 minutes

1. Check Grafana overview: request rate, P95, 5xx, playback create errors, billing sync errors.
2. Check Prometheus alerts: service down, latency spike, login errors, postgres / redis health.
3. Check recent deploy, migration, or manual admin operation.
4. Open [release-watchlist.md](/Z:/Projects/DramaFlow/docs/release-watchlist.md) if incident happens during rollout.

## Priority order

- billing / entitlement / playback first
- admin mutations second
- feed / content / progress third

## Quick commands

- `docker compose -f backend/deployments/docker-compose/docker-compose.yml ps`
- `curl http://localhost:8088/health/live`
- `curl http://localhost:8085/health/live`
- `curl http://localhost:8087/health/live`
- `sh backend/deployments/scripts/release_watch.sh`
- `sh backend/deployments/scripts/rollback_verify.sh`

## Stop-loss rule

If billing sync, entitlement convergence, or playback create is red at the same time during rollout, freeze rollout first and rollback second. Do not debug at full traffic.
