# Android Local App Go / No-Go

- Decision: `No-go`
- Booted target available: `yes`
- APK installed: `yes`
- App launched: `yes`
- Feed / Detail: `pass`
- Playback: `pass`
- Billing / Entitlement: `fail`
- Revoke / Restore: `fail`

## Reason
- Local App smoke was executed on a physical Pixel 6 Pro.
- The remaining blocker is a real contract mismatch in entitlement restore, not device availability and not Docker runtime.
