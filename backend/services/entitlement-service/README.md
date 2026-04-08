# entitlement-service

`entitlement-service` is the entitlement source of truth for DramaFlow Phase 5.

Responsibilities:

- store and audit entitlement state
- answer playback-access checks
- expose `/v1/entitlements/me` for Android refresh
- accept internal grant / revoke / recompute requests from billing-service

This service intentionally does not verify Google Play purchases. Verification, RTDN ingestion, and purchase-record synchronization live in `billing-service`.
