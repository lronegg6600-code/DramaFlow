# Android x Backend Defect Matrix

## Open Defects
- None confirmed on the local app path after the Phase 28 retest.

## Resolved Defects
| id | title | area | severity | suspected_owner | blocker_for_staging | blocker_for_production | fix_strategy |
| --- | --- | --- | --- | --- | --- | --- | --- |
| APP-LOCAL-001 | Restore purchase crashes when entitlement-service returns `entitlements=null` | Contract mismatch | P1 | android + backend contract owners | no | no | Backend source now emits `[]` in repo and Android now tolerates `null`; physical-device restore no longer crashes |
