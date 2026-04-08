# Secret Inventory Template

## Hard Secrets

| Key | Environment | Owner | Consumer | Blocking |
| --- | --- | --- | --- | --- |
| `DRAMAFLOW_POSTGRES_DSN` | staging / production | platform/security | all backend services | yes |
| `DRAMAFLOW_REDIS_ADDR` | staging / production | platform/security | all backend services | yes |
| `DRAMAFLOW_JWT_SECRET` | staging / production | platform/security | auth/admin services | yes |
| `ADMIN_SESSION_SECRET` | staging / production | platform/security | admin-service | yes |
| `ADMIN_BOOTSTRAP_PASSWORD` | staging / production | release manager / security | admin-service | yes |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | staging / production | platform/security | billing-service | yes |
| `BILLING_RTDN_PUSH_SECRET` | staging / production | platform/security | billing-service | yes |
| `DRAMAFLOW_REGISTRY_USERNAME` | staging / production | platform/registry admin | CI/deploy workflow | yes |
| `DRAMAFLOW_REGISTRY_PASSWORD` | staging / production | platform/registry admin | CI/deploy workflow | yes |
| `DRAMAFLOW_CLOUD_SERVICE_ACCOUNT` | staging / production | platform/security | deploy workflow | yes |
| `DRAMAFLOW_DEPLOY_TOKEN` | staging / production | repo admin / platform | deploy workflow | yes |

## Validation

Run:

- `verify_secret_readiness.sh`

If any listed key is absent, real staging execution remains blocked.
