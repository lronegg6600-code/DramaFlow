# billing-service

`billing-service` owns the Google Play sync, verification, RTDN ingestion, purchase records, and order synchronization flow for DramaFlow Phase 5.

Responsibilities:

- accept Android purchase token sync
- verify subscriptions with the Google Play Publisher abstraction
- ingest and de-duplicate RTDN
- keep `billing_purchase_records`, `billing_orders`, `billing_rtdn_events`
- call `entitlement-service` to grant or revoke access

It is intentionally not the entitlement source of truth. That boundary remains in `entitlement-service`.
