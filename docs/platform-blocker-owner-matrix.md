# Platform Blocker Owner Matrix

Generated at: 2026-04-08T04:05:47.981Z

## Summary

- Total blockers: 41
- not_received: 41
- received_but_invalid: 0
- received_and_verified: 0

## Matrix

| blocker_id | category | status | owner | due | dependency |
| --- | --- | --- | --- | --- | --- |
| RI-01 | Repository identity | not_received | repo admin | same_day | - |
| RI-02 | Repository identity | not_received | repo admin | same_day | RI-01 |
| RI-03 | Repository identity | not_received | repo admin | same_day | RI-01 |
| RI-04 | Repository identity | not_received | repo admin | same_day | RI-02 |
| RI-05 | Repository identity | not_received | repo admin | next_business_day | RI-02 |
| RI-06 | Repository identity | not_received | repo admin | next_business_day | RI-05 |
| RI-07 | Repository identity | not_received | release manager | same_day | RI-01 |
| AI-01 | Artifact identity | not_received | registry admin | same_day | RI-02 |
| AI-02 | Artifact identity | not_received | registry admin | same_day | AI-01 |
| AI-03 | Artifact identity | not_received | platform | same_day | RI-02 |
| AI-04 | Artifact identity | not_received | platform | same_day | RI-02 |
| AI-05 | Artifact identity | not_received | platform | same_day | AI-04 |
| AI-06 | Artifact identity | not_received | platform | same_day | - |
| AI-07 | Artifact identity | not_received | repo admin | next_business_day | AI-02 |
| CA-01 | Cluster access | not_received | platform | same_day | - |
| CA-02 | Cluster access | not_received | platform | same_day | CA-01 |
| CA-03 | Cluster access | not_received | platform | same_day | CA-01 |
| CA-04 | Cluster access | not_received | platform | same_day | CA-03 |
| CA-05 | Cluster access | not_received | platform | same_day | CA-01 |
| CA-06 | Cluster access | not_received | platform | same_day | CA-05 |
| CA-07 | Cluster access | not_received | platform | same_day | CA-05 |
| GH-01 | GitHub release control | not_received | release manager | same_day | - |
| GH-02 | GitHub release control | not_received | release manager | next_business_day | - |
| GH-03 | GitHub release control | not_received | release manager | same_day | GH-01 |
| GH-04 | GitHub release control | not_received | release manager | same_day | GH-01 |
| GH-05 | GitHub release control | not_received | release manager | same_day | GH-01 |
| GH-06 | GitHub release control | not_received | platform | same_day | GH-01 |
| GH-07 | GitHub release control | not_received | release manager | next_business_day | GH-01 |
| SC-01 | Secrets / config | not_received | ops | same_day | - |
| SC-02 | Secrets / config | not_received | ops | same_day | - |
| SC-03 | Secrets / config | not_received | ops | same_day | - |
| SC-04 | Secrets / config | not_received | ops | same_day | - |
| SC-05 | Secrets / config | not_received | security | same_day | - |
| SC-06 | Secrets / config | not_received | billing owner | same_day | - |
| SC-07 | Secrets / config | not_received | billing owner | same_day | - |
| SC-08 | Secrets / config | not_received | platform | same_day | AI-06 |
| SC-09 | Secrets / config | not_received | platform | same_day | SC-08 |
| SC-10 | Secrets / config | not_received | platform | same_day | SC-08 |
| SC-11 | Secrets / config | not_received | ops | same_day | - |
| SC-12 | Secrets / config | not_received | platform | same_day | - |
| DT-01 | Deployment tooling | not_received | ops | same_day | CA-01 |
