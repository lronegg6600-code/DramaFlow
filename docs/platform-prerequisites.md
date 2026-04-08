# Platform Prerequisites

## Goal

Turn the current environment blockers into a concrete list of prerequisites that platform, DevOps, repository admins, and release managers can satisfy without repeated clarification.

## Current State

- business and release smoke paths are green locally
- production is still blocked by environment and identity evidence

## Hard Prerequisites

| Category | Requirement | Provided by | Blocks staging | Blocks production | Verification |
| --- | --- | --- | --- | --- | --- |
| Repository identity | real `.git` checkout with commit SHA, branch, tag/version | repo owner / release manager | yes | yes | `verify_repo_identity.sh` |
| Artifact identity | image tag, digest, admin artifact id, workflow run id | CI/platform | yes | yes | `verify_artifact_identity.sh` |
| Cluster access | kubeconfig, current-context, namespace, RBAC role | platform/ops | yes | yes | `verify_cluster_access.sh` |
| Deploy tooling | `kubectl`, `helm`, values overlays, registry rules | platform/ops | yes | yes | `verify_deploy_tooling.sh` |
| GitHub release control | environments, reviewers, self-review policy, deployment policy | repo admin / release manager | no | yes | `verify_github_environment_setup.sh` |
| Secrets/config | env secrets, service account creds, registry creds | platform/security | yes | yes | `verify_secret_readiness.sh` |

## Engineering-Side Ready Items

- release smoke and integration flows are already executable
- release evidence pack structure exists
- rehearsal/canary/rollback/soak scripts exist
- workflows can upload artifacts once environment permissions are supplied

## External Dependencies

- repository checkout with `.git`
- cluster access
- Helm installation or approved alternative
- registry credentials
- GitHub environment configuration
- secret injection path
