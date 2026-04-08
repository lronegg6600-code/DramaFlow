# Billing Incident Playbook

## Symptoms

- purchase sync error spike
- RTDN duplicate / processed mismatch
- entitlement not granted after successful sync

## Triage

1. Check `billing_purchase_sync_error_total`
2. Check `billing_rtdn_received_total` vs `billing_rtdn_processed_total`
3. Check admin purchase resync and audit logs

## Mitigation

- pause risky admin bulk operations
- use purchase resync on affected tokens
- if verifier regression is confirmed, switch traffic to degraded manual sync flow

## Recovery verification

- purchase sync returns 200
- entitlement becomes active
- playback access moves to `full_access`
