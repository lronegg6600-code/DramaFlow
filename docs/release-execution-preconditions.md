# Release Execution Preconditions

## Engineering Preconditions

- release smoke green
- critical integration flows green
- defect matrix reviewed
- blocker list current

## Platform Preconditions

- cluster context available
- namespace and RBAC confirmed
- Helm/registry/deploy tooling confirmed
- secrets injected

## Release Manager Preconditions

- candidate SHA/tag/artifacts recorded
- approvers assigned
- GitHub environments configured
- evidence artifact upload path confirmed

## Current State - 2026-04-08

- engineering preconditions: mostly ready
- platform preconditions: blocked
- release control preconditions: blocked

## Shortest Path To Real Staging

1. provide git-backed RC checkout
2. provide staging kube context and namespace
3. provide staging registry/deploy credentials
4. confirm GitHub staging environment and reviewers
5. inject staging secrets
6. run `bootstrap_staging_access_check.sh`
7. only then rerun real staging rehearsal

## Blocking Rule

If platform or release control preconditions are not met, do not schedule real staging rehearsal.
