# Android Billing / Restore Retest Report

## Scope
- Device: `Pixel 6 Pro`
- Serial: `1A071FDEE00538`
- APK: [app-debug.apk](/Z:/Projects/DramaFlow/android/app/build/outputs/apk/debug/app-debug.apk)

## Result
- Billing / Entitlement: `pass`
- Restore / Revoke: `pass`

## What Changed
- Before fix: tapping `Restore purchase` crashed the app with `JsonDecodingException`.
- After fix: the same physical-device flow stays inside the subscription UI and shows:
  - `Purchase status`
  - `Restore synced 0 purchase(s).`

## Evidence
- Screenshot: [phase28-subscription-top.png](/Z:/Projects/DramaFlow/android/integration/evidence/screenshots/phase28-subscription-top.png)
- Screenshot: [phase28-subscription-lower-after-fix.png](/Z:/Projects/DramaFlow/android/integration/evidence/screenshots/phase28-subscription-lower-after-fix.png)
- Screenshot: [phase28-restore-after-fix.png](/Z:/Projects/DramaFlow/android/integration/evidence/screenshots/phase28-restore-after-fix.png)
- UI dump: [uidump-phase28-after-restore.xml](/Z:/Projects/DramaFlow/android/integration/evidence/uidump-phase28-after-restore.xml)
- Logcat: [phase28-post-revoke.log](/Z:/Projects/DramaFlow/android/integration/evidence/logcat/phase28-post-revoke.log)

## Residual Note
- The live localhost entitlement-service process still emits `entitlements: null`.
- The app no longer crashes because Android now tolerates and normalizes that payload.
