# DramaFlow Phase 0 Foundation

## 1. Phase Goal

Freeze the product shape, commercial model, system boundaries, engineering stack, repository layout, core SLOs, data model language, and compliance red lines before writing Android, backend, and admin code.

## 2. Key Decisions And Reasons

### Product Definition

- Product codename: DramaFlow
- Positioning: an original overseas vertical short-drama app centered on immersive episode chaining, monetized unlocks, subscriptions, and personalized continuation.
- Creative direction: bright, cinematic, energetic visual language with original brand assets; no direct reuse of competitor branding, copy, resource names, API schemas, or event names.
- Market entry: English-first, global-ready, locale-aware from day one.

### Recommended Commercial Model

- Candidate A: subscription only
- Candidate B: subscription plus episode unlock
- Candidate C: subscription plus coins plus rewarded ads
- Recommended primary model: subscription plus episode unlock

Reasons:

- It fits overseas short-drama habits better than subscription-only because many users want to sample one title without committing to a monthly plan.
- It converts better than coins plus ads at launch because rewarded-ad economics and compliance overhead are higher, and ad-heavy flows can hurt premium retention.
- It lets operations run first-purchase bundles, discounted unlock packs, and subscription upsell without splitting entitlement logic.

Cold-start guidance:

- Content scarcity: launch with a smaller but tightly curated catalog, genre landing pages, strong editorial shelves, and a completion-driven rotation strategy.
- Payment conversion: free trial episodes, first purchase bonus packs, timed discounts on cliffhanger gates, and subscription upsell after repeated unlock behavior.
- Retention: episode auto-continue, resume shelf, push triggers on unfinished series, and next-title recommendations at finale completion.
- Recommendation quality: start from editorial plus popularity plus tag affinity, then gradually blend watch completion, rewatch, unlock conversion, and subscription response.

### Architecture

- Android app: Kotlin, Jetpack Compose, Hilt, Coroutines/Flow, Room, DataStore, Paging 3, Retrofit/OkHttp, Kotlinx Serialization, Media3, Billing, Firebase, Baseline Profiles.
- Backend runtime: Go with Gin.
- Admin console: Next.js with TypeScript, Tailwind CSS, shadcn/ui.
- Infra: PostgreSQL, Redis, Kafka, S3-compatible object storage, CDN, Kubernetes, Helm, GitHub Actions, OpenTelemetry.

Why Gin instead of Fiber:

- Gin has broader enterprise adoption, simpler middleware interoperability, clearer handler patterns for mixed REST and internal service environments, and lower migration friction for teams hiring general Go backend engineers.
- Fiber is faster in some benchmarks, but Gin is sufficiently fast for this workload when paired with Redis, CDN, and proper caching.
- Switch to Fiber only if internal profiling shows framework overhead is material relative to business logic and JSON costs.

## 3. Deliverables

- Original product definition
- Commercial model recommendation
- System architecture diagram
- Android and backend technology choices
- Repository directory tree
- Key SLOs
- Core domain data model overview
- Google Play and compliance red lines
- Risk register

## 4. Architecture Diagram

```text
                                  +---------------------------+
                                  |      Admin Web (Next.js)  |
                                  +-------------+-------------+
                                                |
                                                v
+----------------+      HTTPS       +-----------+------------+      gRPC/REST      +----------------------+
| Android Client | <--------------> | API Gateway / Edge API | <-----------------> | Internal Go Services  |
| Compose + M3   |                  | auth, routing, limits  |                     | auth/content/feed/... |
+-------+--------+                  +-----+---------+--------+                     +----+----+----+-------+
        |                                 |         |                                   |    |    | 
        | HLS / signed manifest           |         |                                   |    |    |
        v                                 |         |                                   |    |    |
+-------+---------------------+           |         |                                   |    |    |
| CDN (CloudFront equivalent) | <---------+         |                                   |    |    |
+-------+---------------------+                     |                                   |    |    |
        |                                           |                                   |    |    |
        v                                           v                                   v    v    v
+-------+-------------------+            +----------+---------+               +---------+----+ +----------------+
| Object Storage (S3)       |            | Redis Cache Layer  |               | PostgreSQL    | | Kafka/Event Bus|
| HLS manifests/segments     |            | feed, entitlement, |               | source of     | | async fanout   |
+---------------------------+            | hot titles, tokens |               | truth         | +--------+-------+
                                        +----------+---------+               +------+--------+          |
                                                   |                                |                   |
                                                   |                                |                   v
                                                   |                                |          +--------+------------------+
                                                   |                                |          | Recommendation / ETL /    |
                                                   |                                |          | Analytics / Notification  |
                                                   |                                |          +---------------------------+

Upload/Media Pipeline:

Studio upload -> content-service -> object storage ingest bucket -> transcode job -> HLS ABR outputs ->
DRM/signed playback metadata -> CDN cache warm -> publish event -> feed/recommendation refresh
```

### Sync Paths

- App -> API gateway -> auth-service for login, refresh, logout, account deletion
- App -> feed-service for home shelves, cards, recommendations
- App -> content-service for title detail, episode lists, metadata
- App -> playback-service for signed playback authorization
- App -> progress-service for resume and watch progress
- App -> billing-service for purchase submission
- App -> entitlement-service for current access rights
- Admin -> admin-service -> content/feed/pricing/publication configuration

### Async Paths

- billing-service -> Kafka -> entitlement-service for durable grant pipeline
- content-service -> Kafka -> feed-service and recommendation workers on publish/unpublish
- playback/progress/client events -> Kafka -> analytics warehouse / recommendation features
- moderation or DMCA takedown event -> Kafka -> content unpublish, CDN purge, audit trail

### Cache Layers

- Edge CDN: HLS playlists, segments, posters
- Redis: feed cards, hot title metadata, entitlement snapshots, signed token state, rate limits
- Device local cache: Room for feed snapshots, detail metadata, history, progress shadow

### Strong Consistency

- Purchase validation result
- Entitlement status read after successful purchase restore/verify
- Account deletion state
- Content unpublish/takedown enforcement

### Eventual Consistency Acceptable

- Home feed ordering
- Recommendation refresh
- Analytics dashboards
- Popularity counters
- Non-critical watch-behavior aggregations

## 5. Repository Layout

```text
DramaFlow/
├─ docs/
│  ├─ phase-0-foundation.md
│  ├─ architecture/
│  │  ├─ system-context.md
│  │  ├─ api-boundaries.md
│  │  ├─ playback-pipeline.md
│  │  └─ slo-sli.md
│  ├─ product/
│  │  ├─ information-architecture.md
│  │  ├─ monetization.md
│  │  ├─ growth-mvp.md
│  │  └─ content-operations.md
│  ├─ compliance/
│  │  ├─ privacy-data-map.md
│  │  ├─ account-deletion-flow.md
│  │  ├─ dmca-process.md
│  │  └─ play-release-checklist.md
│  └─ runbooks/
│     ├─ incident-response.md
│     ├─ oncall.md
│     └─ rollback.md
├─ android/
│  ├─ settings.gradle.kts
│  ├─ build.gradle.kts
│  ├─ gradle/
│  │  └─ libs.versions.toml
│  ├─ app/
│  ├─ core/
│  │  ├─ common/
│  │  ├─ model/
│  │  ├─ network/
│  │  ├─ database/
│  │  ├─ datastore/
│  │  ├─ ui/
│  │  ├─ designsystem/
│  │  ├─ player/
│  │  ├─ billing/
│  │  ├─ analytics/
│  │  ├─ testing/
│  │  └─ navigation/
│  ├─ feature/
│  │  ├─ auth/
│  │  ├─ onboarding/
│  │  ├─ feed/
│  │  ├─ detail/
│  │  ├─ player/
│  │  ├─ subscription/
│  │  ├─ profile/
│  │  ├─ history/
│  │  ├─ library/
│  │  └─ settings/
│  ├─ sync/
│  └─ benchmark/
├─ backend/
│  ├─ go.work
│  ├─ shared/
│  │  ├─ config/
│  │  ├─ observability/
│  │  ├─ middleware/
│  │  ├─ auth/
│  │  ├─ idempotency/
│  │  ├─ pagination/
│  │  ├─ kafka/
│  │  ├─ redis/
│  │  ├─ postgres/
│  │  └─ signing/
│  ├─ services/
│  │  ├─ api-gateway/
│  │  ├─ auth-service/
│  │  ├─ content-service/
│  │  ├─ feed-service/
│  │  ├─ playback-service/
│  │  ├─ entitlement-service/
│  │  ├─ billing-service/
│  │  ├─ progress-service/
│  │  ├─ recommendation-service/
│  │  ├─ notification-service/
│  │  └─ admin-service/
│  ├─ jobs/
│  │  ├─ transcode-dispatcher/
│  │  ├─ analytics-consumer/
│  │  ├─ entitlement-reconciler/
│  │  └─ catalog-publisher/
│  ├─ deployments/
│  │  ├─ docker/
│  │  ├─ compose/
│  │  ├─ helm/
│  │  └─ k8s/
│  ├─ api/
│  │  ├─ openapi/
│  │  └─ protobuf/
│  ├─ sql/
│  │  ├─ migrations/
│  │  ├─ seeds/
│  │  └─ queries/
│  └─ tests/
│     ├─ contract/
│     ├─ integration/
│     └─ load/
├─ admin/
│  ├─ package.json
│  ├─ src/
│  │  ├─ app/
│  │  ├─ components/
│  │  ├─ features/
│  │  ├─ lib/
│  │  ├─ hooks/
│  │  └─ types/
│  └─ public/
├─ infra/
│  ├─ terraform/
│  ├─ helmfile/
│  ├─ env/
│  │  ├─ dev/
│  │  ├─ staging/
│  │  └─ prod/
│  └─ scripts/
├─ .github/
│  └─ workflows/
└─ tools/
   ├─ codegen/
   ├─ mock-server/
   └─ release/
```

## 6. Key SLOs

| Domain | SLI | Target |
|---|---|---|
| Feed first screen | client-perceived ready time P50 | <= 1.2s on warm path |
| Feed first screen | client-perceived ready time P95 | <= 2.5s |
| Playback first frame | P50 | <= 900ms |
| Playback first frame | P95 | <= 2.0s |
| Auto next episode seamless switch | success rate | >= 97.5% |
| Paid entitlement activation | purchase success to entitlement ready P95 | <= 3s |
| Progress durability | lost progress rate per 10k sessions | < 5 |
| API availability | monthly | >= 99.95% |
| Hot title CDN cache hit | top 1% titles | >= 98% |
| Crash-free sessions | Android release | >= 99.7% |
| Playback authorization | p95 | <= 150ms internal, <= 400ms edge |
| Billing callback reconciliation | 24h compensation success | >= 99.9% |

## 7. Core Data Model Overview

### Identity And User

- `users`: user account, locale, lifecycle state, age gate flags
- `devices`: app install, attestation summary, risk metadata snapshot
- `sessions`: refresh token lineage, device binding, revocation state

### Content

- `titles`: drama series metadata, publish state, territory policy, content rating
- `seasons`: optional grouping abstraction for future extensions
- `episodes`: per-episode metadata, order, free/paid gate, runtime, poster
- `media_assets`: source mezzanine assets and transcode job linkage
- `playback_variants`: HLS playlists, renditions, subtitles, poster, status
- `collections`: editorial shelves, campaigns, featured rows

### Commerce

- `products`: sellable entities, subscription or unlock pack
- `plans`: regional price plans, offer windows, currencies
- `orders`: purchase attempts and states
- `receipts`: store or server-side evidence records
- `entitlements`: effective access grants with source and expiration
- `renewals`: subscription renewal lifecycle
- `revocations`: refund, chargeback, abuse revokes

### Engagement

- `watch_progress`: per user/title/episode progress with heartbeat version
- `watch_history`: recent playback records for profile surfaces
- `favorites`: saved titles
- `recommendation_impressions`: served cards and ranking trace
- `analytics_events`: client/server events, partitioned for export

### Operations

- `experiments`: experiment definitions and rollout rules
- `feature_flags`: kill switches and staged rollout config
- `audit_logs`: immutable admin operation records
- `takedown_cases`: copyright and compliance workflows

## 8. Compliance And Store Red Lines

- Do not bypass Google Play Billing for digital content in the Play build.
- Do not ship account creation without both in-app and web account deletion entry points.
- Do not declare Data Safety categories that are broader or narrower than actual code behavior.
- Do not collect device fingerprint or background identifiers beyond disclosed, legally justified, minimal scope.
- Do not publish copyrighted titles without documented rights ownership or license traceability.
- Do not expose adult or sensitive content without rating, territory gates, moderation policy, and complaint channel.
- Do not retain purchase, progress, or PII logs without retention policy and access controls.
- Do not leave admin actions unaudited.
- Do not hardcode secrets, signing keys, service accounts, or store credentials in repos or apps.

## 9. Risk Register

| Risk | Why It Matters | Early Mitigation |
|---|---|---|
| Overbuilding recommendation too early | delays launch while cold-start data is weak | start with editorial + popularity + tags |
| Entitlement race conditions | user pays but cannot watch | server truth, client optimistic retry, reconciler |
| Black screen on auto-next | kills binge behavior | preload next manifest and decoder warm-up path |
| Weak-network first frame regressions | high early churn | short GOP, poster placeholder, low-bitrate bootstrap |
| Hot title traffic spikes | cache miss storms and token bottlenecks | multi-layer cache, hot shard isolation, rate limits |
| Admin tooling lagging behind content ops needs |运营无法快速调价、上下架、调推荐位| treat admin as primary product, not support panel |
| Compliance gaps for deletion/privacy | Play rejection or legal exposure | build deletion/data export contract in phase 1-3 |
| Analytics schema drift | recommendation and BI breakage | shared event catalog and versioned schemas |
| Multi-region data issues | latency and residency complexity | start single write region, edge cache globally |
| Cost blowout on transcode/CDN | unit economics collapse | bitrate ladder discipline, CDN tiering, cache warm strategy |

## 10. Run Notes

- Recommended Android minSdk: 26. This balances modern API availability, Media3 behavior, and manageable device coverage for overseas paid-content markets.
- Build flavors: `debug`, `staging`, `release`.
- Payment abstraction: `PaymentProvider` with `GooglePlayProvider` active in Play flavor and alternative implementations gated by region and distribution channel.
- Trust boundaries:
  - Client state is advisory.
  - Entitlement-service is the commerce source of truth.
  - Content-service is the catalog source of truth.
  - Progress-service owns resumable playback state.

## 11. Validation For Phase 0

- Architecture review across Android, backend, media, payment, and ops stakeholders
- Event and data model naming review before codegen
- Compliance review for deletion, privacy, billing, copyright, and content rating
- Cost review for CDN, transcode, storage, and Kafka retention

## 12. Next Phase Goal

Phase 1 will create the Android foundation:

- Gradle version catalog
- app and core modules
- design system and navigation
- fake repository layer with stable UI state contracts
- first-pass Feed, Detail, Player, Subscription, and Profile surfaces wired with mock data
