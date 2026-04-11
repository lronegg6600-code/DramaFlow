# Android Post-Cutover Retest Report

## Scope
- Device: `Pixel 6 Pro`
- Serial: `1A071FDEE00538`
- Type: post-cutover minimal regression on the live localhost runtime

## Billing / Entitlement
- Result: `pass`
- Subscription screen still opens after the runtime cutover.
- Billing UI still renders.
- No crash.
- No JSON decode error.
- The visible `Billing product is unavailable.` message remains a local billing-environment limitation, not a contract regression.

## Restore / Revoke
- Result: `pass`
- Restore entry remained reachable after the runtime cutover.
- Minimal restore / revoke regression stayed stable in the local free-tier scenario.
- No crash.
- No `JsonDecodingException`.
- No `ECONNREFUSED` after `adb reverse` was restored.

## Interpretation
This post-cutover retest proves the device result is no longer relying only on Android null tolerance. The live backend runtime now returns `entitlements=[]`, and the device remained stable on top of that runtime.
