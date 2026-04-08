# Staging Access Checklist

## Objective

Define the minimum conditions required before Phase 9 real staging rehearsal can be re-run as an actual staging execution.

## Checklist

- [ ] real repository checkout contains `.git`
- [ ] candidate SHA is recorded
- [ ] staging kubeconfig is provided
- [ ] `kubectl config current-context` resolves to `dramaflow-staging`
- [ ] namespace is known and exported as `DRAMAFLOW_KUBE_NAMESPACE`
- [ ] release operator has RBAC allowing rollout status, rollout restart/undo, and pod/log inspection
- [ ] staging registry pull credentials are available in cluster
- [ ] staging deploy token / workflow identity is available
- [ ] Helm is installed or an approved kubectl-only deployment path is documented
- [ ] staging environment secrets are provisioned
- [ ] Prometheus / Grafana / logs endpoints are reachable
- [ ] GitHub `staging` environment exists and reviewer chain is configured

## Validation Commands

- `sh backend/deployments/scripts/bootstrap_staging_access_check.sh`
- `kubectl config current-context`
- `kubectl auth can-i get pods -n dramaflow-staging`
- `kubectl auth can-i update deployment/admin-service -n dramaflow-staging`

## Blocking Rule

If any item above is missing, real staging rehearsal remains blocked.
