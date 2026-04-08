# Operator Handoff Pack

## Audience

- platform engineer
- DevOps / SRE
- release manager
- engineering owner

## What This Pack Enables

- close environment blockers without re-asking engineering for definitions
- prepare staging/prod access
- validate whether real rehearsal can start

## Required Inputs From Platform

1. repository-backed RC checkout
2. staging kubeconfig/context
3. production kubeconfig/context
4. Helm installation or approved alternative
5. registry credentials
6. environment secrets
7. GitHub environments with reviewers and policies

## Required Inputs From Engineering

1. candidate release version naming
2. impacted services list
3. smoke and integration commands
4. rollout order and rollback order
5. watchlist and stop conditions

## Hand-off Execution Order

1. run `bootstrap_staging_access_check.sh`
2. close blockers from generated evidence JSON files
3. run real staging rehearsal
4. run canary drill
5. run rollback drill
6. run soak or formal waiver flow
7. rerun production gate
