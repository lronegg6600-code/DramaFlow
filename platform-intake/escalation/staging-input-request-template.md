# Staging Input Request

Please provide the 7 staging base URLs in `platform-intake/received/staging-inputs/manifests/staging-base-urls.yaml`.

Required fields:
- `authBaseUrl`
- `contentBaseUrl`
- `feedBaseUrl`
- `progressBaseUrl`
- `playbackBaseUrl`
- `entitlementBaseUrl`
- `billingBaseUrl`

Acceptance rule:
- all 7 URLs must be valid `https://` or `http://` URLs
- the package is only accepted when all 7 fields are present and valid

Impact if not provided:
- Android x backend integration rerun remains blocked
- staging rehearsal evaluation cannot continue
