# playback-service

Phase 4 introduces playback authorization as its own service so the player stops guessing local rules.

Current responsibilities:

- issue short-lived playback sessions
- return playback mode (`preview` or `full`)
- sign short-lived media URLs
- accept heartbeat, refresh, and completion calls
- bridge to a temporary dev entitlement source

Out of scope for this phase:

- production entitlement-service
- billing validation
- DRM
- signed cookies
- transcoding or upload pipelines
