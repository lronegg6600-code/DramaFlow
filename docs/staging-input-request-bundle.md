# Staging Input Request Bundle

## Request
Please provide the 7 staging base URLs needed to resume Android x backend integration.

## Required Fields
- `authBaseUrl`
- `contentBaseUrl`
- `feedBaseUrl`
- `progressBaseUrl`
- `playbackBaseUrl`
- `entitlementBaseUrl`
- `billingBaseUrl`

## Drop Location
- `platform-intake/received/staging-inputs/manifests/staging-base-urls.yaml`

## Validation Rule
- all 7 fields must be present
- each field must be a valid `http://` or `https://` URL

## Next Action After Delivery
- export `.env.staging.mobile`
- rerun Android x backend contract smoke
- rerun 4 mobile flows
