# Release Risk Register

## Current Release Candidate Position

- Candidate status: `staging-ready with blockers for production`
- Phase 8 rehearsal status: `local surrogate rehearsal complete; production evidence incomplete`
- Phase 9 real-environment status: `real environment attempt blocked by missing git identity and missing cluster context/credentials`
- Phase 10 enablement status: `blockers mapped to executable checks and handoff materials; access still blocked`
- Decision owner: engineering lead
- Last reviewed: 2026-04-08
- Local blocker status:
  - schema drift in persisted Postgres volume: fixed
  - billing sync false-negative 500 on timestamptz scan: fixed
  - admin purchase lookup 500 on release token query: fixed
  - revoke -> downgrade rehearsal using dev-only path: fixed

## High Risks

| risk_id | risk | likelihood | impact | mitigation | exit_criteria |
| --- | --- | --- | --- | --- | --- |
| RR-01 | Billing sync writes succeed but entitlement/playback do not converge in time | medium | critical | Run `billing_entitlement_playback_flow.mjs` and `entitlement_revoke_access_downgrade.mjs` in staging before every RC | Both flows pass on the target commit and release watchlist stays green for 15 minutes |
| RR-02 | Dangerous admin operations succeed but audit trail is missing or delayed | medium | high | Run `admin_audit_flow.mjs`, check audit log board, keep admin audit alert/watchlist active | Audit flow passes and audit log write metrics are visible in Prometheus/Grafana |
| RR-03 | Feed publish is accepted in admin but downstream feed stays stale | medium | high | Run `feed_publish_visibility.mjs`, watch feed latency/error graphs, verify config version change | Publish flow passes twice in staging and feed dashboard shows no error spike |
| RR-04 | Release operator uses wrong env/config during staging or production promotion | medium | high | Use env templates, staging deploy runbook, release manifest, and pre-deploy checks | Pre-deploy checks pass and manifest is signed with env hash / affected services |
| RR-05 | Rollback is delayed because people know the script name but not the stop-loss order | medium | critical | Keep rollback guide + incident playbooks + canary plan together; practice rollback drill using scripts in this phase | One staging drill completed and rollback verification script passes |
| RR-06 | Alerts fire without clear first action and the first 15 minutes are wasted | medium | high | Add runbook links, suggested first action, and release watchlist doc | Alert annotations and watchlist docs are loaded into Prometheus/Grafana config |
| RR-07 | Candidate identity cannot be proven because the local workspace has no `.git` metadata | high | critical | Do not sign production from this checkout; regenerate the RC from a real repository clone and record the SHA | RC manifest contains exact commit SHA and approvers can trace it to build artifacts |
| RR-08 | Local rehearsal is strong enough for staging preparation but not for production evidence | high | critical | Treat current rehearsal as surrogate-only and require one real staging run before production | Signed staging rehearsal report exists with real environment name, image tags, and outputs |
| RR-09 | Soak evidence remains substitute-grade only | medium | high | Block production until true soak is run or risk is explicitly waived by leadership | True soak evidence or formal waiver is attached to the release evidence pack |
| RR-10 | Real staging execution is blocked because no cluster context or deploy credentials are available in the current environment | high | critical | Do not force production decision from local-only environment; require platform to supply staging context and access | `kubectl config current-context` resolves, namespace is known, and release rehearsal can run against staging |
| RR-11 | Platform handoff ambiguity can delay release even after blockers are identified | medium | high | Use the Phase 10 enablement pack and bootstrap scripts as the mandatory handoff packet | Platform can run bootstrap access check without asking engineering for missing definitions |

## Accepted Risks For Staging

- Lack of real production cluster evidence is acceptable for staging rehearsal only.
- Load testing remains baseline/spike focused; soak evidence can lag production only if release volume remains low and watchlist is staffed.

## Not Accepted For Production

- Any open P0 issue in the defect matrix.
- Any P1 issue marked `release_blocker=yes`.
- No signed staging rehearsal evidence.
- No rollback path verified for migrations included in the candidate.
- No provable RC commit SHA.
- No production-grade canary / rollback / soak evidence.
