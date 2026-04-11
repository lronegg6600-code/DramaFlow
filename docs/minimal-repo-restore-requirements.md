# Minimal Repo Restore Requirements

The minimum restore required before Android x backend integration can begin:

1. Git-backed checkout with `.git`
2. Android source tree with Gradle settings and source modules
3. Backend source tree with `services/` and module files
4. staging base URL inputs for:
   - auth
   - content
   - feed
   - progress
   - playback
   - entitlement
   - billing
