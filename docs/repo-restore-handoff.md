# Repo Restore Handoff

## Owner Split
- repo admin: restore or reclone the authoritative repo
- android owner: verify Android source completeness
- backend owner: verify backend service completeness
- platform: inject staging inputs after restore

## Acceptance
Restore is accepted only when:
- `.git` exists
- Android source tree verifies as `recovered`
- backend source tree verifies as `recovered`
- mobile readiness rerun produces a non-source-blocked result
