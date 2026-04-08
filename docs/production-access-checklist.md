# Production Access Checklist

## Objective

Define the minimum conditions required before production gate can even be requested.

## Checklist

- [ ] real repository checkout contains `.git`
- [ ] candidate SHA, tag/version, image tag, digest, and admin artifact id are recorded
- [ ] production kubeconfig is provided
- [ ] `kubectl config current-context` resolves to `dramaflow-production`
- [ ] namespace is known and exported as `DRAMAFLOW_KUBE_NAMESPACE`
- [ ] production rollout RBAC is granted
- [ ] rollback RBAC is granted
- [ ] Helm is installed and production values overlay is available
- [ ] production registry pull credentials are active
- [ ] production secrets are provisioned through approved secret source
- [ ] GitHub `production` environment exists with required reviewers and self-review prevention
- [ ] canary watch dashboards and alert routes are reachable
- [ ] rollback target artifact is identified
- [ ] soak evidence exists or formal waiver is signed

## Blocking Rule

Missing any hard prerequisite keeps production in `no-go`.
