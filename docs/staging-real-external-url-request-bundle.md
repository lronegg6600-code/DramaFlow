# Real External Staging URL Request Bundle

## Why The Previous Candidate Was Rejected
- first-round repo-derived candidate URLs were received
- all 7 were rejected with the same reason: `hostname did not resolve`
- candidate values therefore cannot be treated as Android-accessible staging endpoints

## What Is Required Now
Provide a second-round manifest containing only real, resolvable, Android-accessible external gateway/domain mappings for:
- `authBaseUrl`
- `contentBaseUrl`
- `feedBaseUrl`
- `progressBaseUrl`
- `playbackBaseUrl`
- `entitlementBaseUrl`
- `billingBaseUrl`

## Format Requirements
- must be full `http://` or `https://` URLs
- must resolve in DNS
- must be reachable from Android client environment
- must point to staging, not dev or prod
- must map to the correct service route
- candidate names such as `https://api.staging.dramaflow.example/...` are not acceptable unless they actually resolve

## Delivery Location
- `platform-intake/received/staging-external-urls/responses/staging-external-urls.yaml`

## Issue Tracking
- issue: `#42`
- current state: first-round candidate invalid closeout completed, second-round request open, second-round reminder sent

## Validation Steps
1. `node tools/ingest_real_external_urls.mjs`
2. `node tools/validate_real_external_urls.mjs`
3. `node tools/export_verified_android_staging_env.mjs`
4. `node tools/rerun_mobile_with_verified_external_urls.mjs`

## Impact If Not Provided
- Android x backend integration remains blocked
- verified Android staging env cannot be exported
- mobile defect burn-down cannot move from environment blocking to runtime defect triage
- next real staging rehearsal evaluation cannot start from the mobile side
