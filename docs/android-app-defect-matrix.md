# Android App Defect Matrix

## Open Defects
- None confirmed after the Phase 28 retest.

## Resolved Defects
| id | title | area | severity | suspected_owner | status | fix_strategy |
| --- | --- | --- | --- | --- | --- | --- |
| APP-LOCAL-001 | Restore purchase crashes when entitlement-service returns entitlements=null | Contract mismatch | P1 | android + backend contract owners | resolved on local app path | Backend source normalizes nil entitlements to `[]`; Android DTO and mapper now normalize `null` to `emptyList()` and no longer crash on device restore. |
