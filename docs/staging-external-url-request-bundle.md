# Staging External URL Request Bundle

## Current Known State
- internal cluster service map is confirmed from repo files
- Android-accessible external staging URLs are still missing
- Android x backend rerun remains blocked until all 7 external URLs are verified

## Required Fields
- `authBaseUrl`
- `contentBaseUrl`
- `feedBaseUrl`
- `progressBaseUrl`
- `playbackBaseUrl`
- `entitlementBaseUrl`
- `billingBaseUrl`

## Format Requirements
- must be complete Android-accessible external URLs
- must be valid `http://` or `https://` URLs
- must point to staging, not dev or prod
- internal service URLs such as `http://auth-service:8081` are not acceptable

## Drop Location
- `platform-intake/received/staging-external-urls/manifests/staging-external-urls.yaml`

## Validation
- `node tools/ingest_external_staging_urls.mjs`
- `node tools/validate_external_staging_urls.mjs`
- `node tools/export_android_staging_env.mjs`
- `node tools/rerun_mobile_after_external_urls.mjs`

## Impact If Not Provided
- Android x backend rerun remains blocked
- mobile defect burn-down cannot start
- real staging rehearsal evaluation cannot advance from the mobile side
