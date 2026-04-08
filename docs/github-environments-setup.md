# GitHub Environments Setup

## Required Environments

- `staging`
- `production`

## Required Settings

| Environment | Required reviewers | Prevent self-review | Deployment policy | Required secrets/vars |
| --- | --- | --- | --- | --- |
| staging | release manager + backend owner | recommended | protected release branches or tags | cluster namespace, control plane vars, evidence artifact settings |
| production | engineering owner + release manager + ops/oncall reviewer | required | protected release tags only | all release vars plus production credentials |

## Required Vars

- `DRAMAFLOW_CONTROL_PLANE`
- `DRAMAFLOW_KUBE_NAMESPACE`
- `DRAMAFLOW_REGISTRY`
- `DRAMAFLOW_RELEASE_VERSION`

## Required Secrets

- registry username/password
- cloud service account
- deploy token
- environment-specific secret references if workflows resolve them directly

## Verification

Use:

- `verify_github_environment_setup.sh`

and set:

- `DRAMAFLOW_GITHUB_STAGING_ENV_READY=1`
- `DRAMAFLOW_GITHUB_PRODUCTION_ENV_READY=1`
- `DRAMAFLOW_GITHUB_REQUIRED_REVIEWERS_READY=1`
- `DRAMAFLOW_GITHUB_PREVENT_SELF_REVIEW=1`
- `DRAMAFLOW_GITHUB_DEPLOYMENT_POLICY_READY=1`
- `DRAMAFLOW_GITHUB_EVIDENCE_ARTIFACTS_READY=1`

Only mark them once repository admins have actually configured the settings.
