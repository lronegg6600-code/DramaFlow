# Helm Values Matrix

## Why This Exists

The chart exists, but release execution was still blocked because environment overlays were not formalized enough to hand over cleanly.

## Files

- [staging.values.example.yaml](/Z:/Projects/DramaFlow/backend/deployments/helm/values/staging.values.example.yaml)
- [production.values.example.yaml](/Z:/Projects/DramaFlow/backend/deployments/helm/values/production.values.example.yaml)

## Ownership Matrix

| Value Group | Example | Owner | Hard blocker |
| --- | --- | --- | --- |
| image tag | `registry.example.com/dramaflow/admin-service:staging-<sha>` | CI/platform | yes |
| image digest | `sha256:<digest>` | CI/platform | yes |
| postgres dsn secret ref | `secret://dramaflow/staging/postgres_dsn` | platform/security | yes |
| google play service account | secret ref | platform/security | yes |
| admin allowed origins | env value | engineering + platform | yes |
| replica counts | `2` / `3` | platform + service owner | no |

## Validation Rule

Do not treat values overlays as ready until:

- image tags and digests are populated
- secret refs are mapped to actual secret stores
- namespace and ingress expectations are agreed with platform
