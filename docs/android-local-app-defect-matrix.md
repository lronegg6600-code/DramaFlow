# Android Local App Defect Matrix

| id | title | area | severity | suspected_owner | blocker_for_staging | blocker_for_production | fix_strategy |
| --- | --- | --- | --- | --- | --- | --- | --- |
| APP-LOCAL-001 | Restore purchase crashes when entitlement-service returns `entitlements=null` | Contract mismatch | P1 | android + backend contract owners | yes | yes | Return `[]` instead of `null`, or make Android deserialization nullable/lenient |
