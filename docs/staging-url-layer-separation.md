# Staging URL Layer Separation

## Internal Layer
Internal service URLs are confirmed from Helm values and Kubernetes service manifests.

Allowed usage:
- cluster-internal service-to-service traffic
- manifest placeholder
- port-forward reference

Forbidden usage:
- Android direct access
- claims about external gateway readiness
- acceptance as verified mobile staging URLs

## External Layer
Android requires external gateway/domain mapping for the same 7 logical services.

Missing today:
- Ingress
- external host
- API gateway route
- public or Android-reachable staging domain mapping
