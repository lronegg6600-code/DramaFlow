# Android Staging URL Usage Policy

## Internal-Only URLs
Internal cluster URLs are allowed only for:
- service-to-service calls inside the cluster
- manifest placeholder and documentation
- port-forward-based debugging

Internal cluster URLs are not allowed for:
- Android direct access
- external mobile smoke rerun
- claims that staging gateway mapping is complete

## External URLs
Android rerun requires 7 external gateway/domain URLs, one for each logical service.

Verification rule:
- every external URL must be present and valid
- only then may `.env.staging.mobile` be exported
- only then may the full mobile rerun start
