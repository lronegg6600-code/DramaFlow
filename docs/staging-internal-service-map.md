# Staging Internal Service Map

The following 7 URLs are confirmed from repository files and are internal-only cluster service addresses.

| Key | Internal URL | Source |
| --- | --- | --- |
| `authBaseUrl` | `http://auth-service:8081` | `backend/deployments/k8s/base/auth-service.yaml` |
| `contentBaseUrl` | `http://content-service:8082` | `backend/deployments/helm/values/staging.values.example.yaml`, `backend/deployments/k8s/base/content-service.yaml` |
| `feedBaseUrl` | `http://feed-service:8083` | `backend/deployments/helm/values/staging.values.example.yaml`, `backend/deployments/k8s/base/feed-service.yaml` |
| `progressBaseUrl` | `http://progress-service:8084` | `backend/deployments/k8s/base/progress-service.yaml` |
| `playbackBaseUrl` | `http://playback-service:8085` | `backend/deployments/helm/values/staging.values.example.yaml`, `backend/deployments/k8s/base/playback-service.yaml` |
| `entitlementBaseUrl` | `http://entitlement-service:8086` | `backend/deployments/helm/values/staging.values.example.yaml`, `backend/deployments/k8s/base/entitlement-service.yaml` |
| `billingBaseUrl` | `http://billing-service:8087` | `backend/deployments/helm/values/staging.values.example.yaml`, `backend/deployments/k8s/base/billing-service.yaml` |

Rules:
- `internal_only = true`
- `verified_from_repo = true`
- `android_external_access = not_verified`
