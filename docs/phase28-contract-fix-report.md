# Phase 28 Contract Fix Report

## Outcome
- `entitlements = null` crash path: `fixed`
- Fix landing: `both`
- Debug APK rebuilt and reinstalled on the same physical device: `yes`
- Billing / Entitlement retest: `pass`
- Revoke / Restore retest: `pass`

## What Was Verified
1. The Android DTO no longer crashes when the backend returns `entitlements: null`.
2. The same Pixel 6 Pro can restore without crashing and stays on the subscription screen.
3. A follow-up revoke no-crash retest keeps the app stable on the free-tier state.

## What Is Still Pending
1. The live localhost entitlement-service process still needs to be restarted from patched source to prove runtime-level `[]` serialization.
2. Staging validation still needs external URLs and is outside this phase.

## Phase 29 Follow-up
- Phase 29 attempted the runtime refresh, but the local Docker/WSL control path timed out before proving instance replacement.
- The local contract state therefore remains `source + android fixed`, not `source + runtime + android fully converged`.

## Phase 30 Follow-up
- Phase 30 identified the 8086 provider as Docker Desktop / WSL forwarding before the cutover attempt.
- Compose cutover then failed immediately because `//./pipe/dockerDesktopLinuxEngine` is missing.
- A Docker Desktop restart attempt did not restore control; it also left localhost listeners on 8081 and 8086 unavailable.
- The contract fix remains valid in source and on Android device retest, but live runtime replacement is now explicitly classified as `runtime replacement blocked`.
