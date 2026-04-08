# Post-Rehearsal Fix Log

## Goal

Capture the fixes and hardening work that were validated during the Phase 8 rehearsal cycle.

## Fixes Confirmed In This Rehearsal Cycle

### 2026-04-08 - release blocker validation

1. Billing purchase sync timestamp scan failure
   - area: `billing-service`
   - result: fixed and revalidated through `billing_entitlement_playback_flow.mjs`

2. Admin purchase lookup timestamp / column drift
   - area: `admin-service`
   - result: fixed and revalidated through release smoke and billing flow

3. Revoke -> playback downgrade rehearsal path
   - area: `entitlement-service` + `playback-service` integration
   - result: fixed and revalidated through `entitlement_revoke_access_downgrade.mjs`

4. Release rehearsal execution chain
   - area: release scripts / workflows / gates
   - result: strengthened with:
     - `verify_staging_gate.mjs`
     - `verify_canary_gate.mjs`
     - `verify_rollback_gate.mjs`
     - `verify_production_gate.mjs`
     - `soak_substitute.mjs`
     - Phase 8 rehearsal scripts and workflows

5. Phase 9 release evidence collection hardening
   - area: release evidence / workflows
   - result: strengthened with:
     - `collect_release_metadata.mjs`
     - real-run / dry-run support in rehearsal scripts
     - evidence directory output support
     - workflow artifact upload and summary output

## No New Runtime P0/P1 Code Defects Found In This Round

This Phase 8 round did not expose a new code-path regression inside the already validated core runtime flows. The remaining blockers are release evidence, environment realism, and sign-off readiness.
