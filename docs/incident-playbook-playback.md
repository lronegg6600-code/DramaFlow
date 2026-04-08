# Playback Incident Playbook

## Symptoms

- `playback_session_create_error_total` spike
- heartbeat / refresh failures
- users see paywall despite entitlement

## Triage

1. Check playback create / refresh error logs by `trace_id`, `session_id`, `episode_id`
2. Confirm entitlement-service health and playback access responses
3. Confirm signer mode and CDN URL generation

## Mitigation

- if signer config breaks, stop release and rollback
- if entitlement path regresses, recompute affected users and verify access
- if only admin lookup fails, keep playback online and fix console separately
