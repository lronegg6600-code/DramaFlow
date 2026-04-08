# DramaFlow Backend

DramaFlow Phase 3 starts the backend as a Go monorepo with four production-facing baseline services:

- `auth-service`
- `content-service`
- `feed-service`
- `progress-service`
- `playback-service`

The current stack intentionally stays narrow:

- Go + Gin
- PostgreSQL + pgx
- Redis
- OpenTelemetry + Prometheus
- Docker + docker-compose
- Helm / Kubernetes base templates

## Why Gin

Gin is the practical choice at this stage because it gives low-friction middleware composition, predictable handler ergonomics, and a hiring-friendly ecosystem. Framework overhead is not the bottleneck for these services yet; clarity and speed of iteration matter more.

## Why pgx instead of GORM or sqlc

Phase 3 uses `pgxpool` with handwritten SQL for three reasons:

- the schema is still small and changes quickly
- repository code stays explicit and easy to inspect during early API iteration
- there is no ORM abstraction cost or generated-query workflow to maintain yet

`sqlc` becomes attractive once queries stabilize and grow. `GORM` is intentionally avoided here because the service boundaries, response envelopes, and schema evolution are clearer with explicit SQL.

## Why only auth/content/feed/progress in Phase 3

These four services establish the minimum product loop:

- create or resume a session
- fetch feed and title metadata
- fetch episodes
- save and read watch progress
- issue short-lived playback sessions and heartbeats

`playback-service`, `billing-service`, and `entitlement-service` are deliberately deferred because they depend on signed delivery, purchase truth, and asynchronous compensation flows that would distract from stabilizing the API foundation.
Phase 4 moves `playback-service` into scope because playback mode, preview enforcement, URL TTL, and auto-next preparation need a server-owned contract before billing becomes useful.

## Why Android only gets minimal network contract work

Android already has a fake-data UX loop that is useful for product iteration. This phase only adds contract-aligned DTOs and Retrofit interfaces so backend and Android can converge on the same schema without destabilizing the current app flow.

## Consistency boundaries

Strong consistency:

- `POST /v1/auth/guest-session`
- `POST /v1/auth/refresh`
- `GET /v1/auth/me`
- `PUT /v1/progress/episodes/{episodeId}`
- `GET /v1/progress/episodes/{episodeId}`

Eventual consistency is acceptable:

- `GET /v1/feed/home`
- `GET /v1/feed/continue-watching`
- aggregate trend ordering
- recommendation placeholders

## Local development

1. Copy `.env.example` to `.env`.
2. Run `make dev-up` from `backend/`.
3. Apply schema with `make migrate-up`.
4. Load sample content with `make seed`.
5. Call service endpoints directly on ports `8081` to `8084`.

The compose stack exposes:

- auth-service: `:8081`
- content-service: `:8082`
- feed-service: `:8083`
- progress-service: `:8084`
- playback-service: `:8085`
- postgres: `:5432`
- redis: `:6379`
- prometheus: `:9090`

Compose files live under `deployments/docker-compose/` and include:

- PostgreSQL
- Redis
- OpenTelemetry Collector
- Prometheus
- auth-service
- content-service
- feed-service
- progress-service
- playback-service

## Phase 4 playback local loop

1. Create a guest session through `auth-service`.
2. Use the returned bearer token to call `POST /v1/playback/sessions`.
3. Start Media3 with the returned `mediaUrl` and `requestHeaders`.
4. Send heartbeat calls to keep the session alive and detect refresh windows.
5. Call refresh before expiry and complete when the episode ends.

For local development, `playback-service` defaults to `dev_passthrough` signing and uses the sample MP4 path seeded into the `episodes` table.

## Validation examples

Create a guest session:

```bash
curl -X POST http://localhost:8081/v1/auth/guest-session \
  -H "Content-Type: application/json" \
  -d '{"anonymousDeviceId":"android-emulator-001"}'
```

Fetch feed:

```bash
curl http://localhost:8083/v1/feed/home
```

Write progress:

```bash
curl -X PUT http://localhost:8084/v1/progress/episodes/df-neon-vows-e4 \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{"dramaId":"df-neon-vows","positionSeconds":12,"durationSeconds":33,"completed":false}'
```

Create playback session:

```bash
curl -X POST http://localhost:8085/v1/playback/sessions \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{"episodeId":"df-neon-vows-e4","sourcePage":"player","autoNext":false,"deviceContext":{"platform":"android","appVersion":"0.4.0-dev","networkType":"wifi"}}'
```

Grant temporary dev premium:

```bash
curl -X POST http://localhost:8085/v1/dev/entitlements/grant-premium \
  -H "Content-Type: application/json" \
  -d '{"userId":"<user-id-from-auth>","entitlementType":"premium"}'
```
## Phase 5 local loop

1. Run `make migrate-up`
2. Run `make seed`
3. Run `docker compose -f deployments/docker-compose/docker-compose.yml up --build`
4. Android subscription sync targets `billing-service` on port `8087`
5. Playback access checks now target `entitlement-service` on port `8086`
6. Local RTDN can be simulated through `POST /v1/dev/billing/mock-rtdn`

## Phase 6 local loop

1. Run `make migrate-up`
2. Run `make seed`
3. Start backend services with `docker compose -f deployments/docker-compose/docker-compose.yml up --build`
4. Open the admin console on `http://localhost:3000`
5. Sign in with `ADMIN_BOOTSTRAP_EMAIL` / `ADMIN_BOOTSTRAP_PASSWORD`
6. Verify drama edits, feed publish / rollback, purchase resync, entitlement recompute, RTDN replay, and playback diagnostics through the admin UI
