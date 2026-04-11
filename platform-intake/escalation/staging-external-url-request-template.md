# Real External Staging URL Request

Current state:
- internal service map is confirmed from repo files
- first-round repo-derived candidate URLs were rejected because all hostnames failed DNS resolution
- Android-accessible real external staging URLs are still required

Required fields:
- `authBaseUrl`
- `contentBaseUrl`
- `feedBaseUrl`
- `progressBaseUrl`
- `playbackBaseUrl`
- `entitlementBaseUrl`
- `billingBaseUrl`

Rules:
- must be Android-accessible real external URLs
- must resolve in DNS
- must point to staging, not dev or prod
- internal service names such as `http://auth-service:8081` are not acceptable for Android rerun
- repo-derived candidate names such as `https://api.staging.dramaflow.example/...` are not acceptable unless they actually resolve
- all 7 URLs must be provided together
