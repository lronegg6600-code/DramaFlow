# Android App Smoke Execution Report

- Feed / Detail: `pass`
- Playback: `pass`
- Billing / Entitlement: `fail`
- Revoke / Restore: `fail`

## Notes
- Billing flow note: Subscription UI rendered, but Restore purchase crashes after entitlement refresh because the response payload contains entitlements=null.
- Revoke/restore note: Restore path is reachable and hits local auth/entitlement services, but the app crashes on entitlement deserialization before state can settle.
