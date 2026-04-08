# DramaFlow Load Test Guide

Phase 7 uses `k6` for baseline, spike and soak-ready load testing.

## Targets

- `POST /v1/playback/sessions`
- `POST /v1/playback/sessions/{id}/heartbeat`
- `POST /v1/billing/google-play/purchases:sync`
- `GET /v1/feed/home`
- `GET /v1/admin/dramas`
- `POST /v1/admin/feed-config/home/publish`

## Run

```bash
k6 run -e BASE_URL=http://localhost:8085 backend/loadtest/k6/playback_sessions.js
k6 run -e BASE_URL=http://localhost:8087 backend/loadtest/k6/billing_sync.js
k6 run -e BASE_URL=http://localhost:8083 backend/loadtest/k6/feed_home.js
```

## Thresholds

- `/v1/feed/home` P95 <= 300ms
- `/v1/playback/sessions` P95 <= 400ms
- `/v1/billing/google-play/purchases:sync` P95 <= 800ms
- admin list / publish endpoints P95 <= 500ms
- HTTP failure rate <= 1%

## Scenarios

- baseline: moderate steady traffic for regression checks
- spike: short burst to verify autoscaling and saturation behavior
- soak: placeholder to extend into 1h+ endurance validation
