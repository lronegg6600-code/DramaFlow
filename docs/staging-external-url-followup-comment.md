## Follow-up: repo-side self-service path exhausted

As of 2026-04-09, engineering has verified the following:

- internal cluster service map is confirmed from repo and k8s service templates
- repo/source recovery is complete; source is no longer the blocker
- this workstation has `kubectl`, but no configured kube context, so repo-side port-forward cannot be used to derive Android-accessible URLs
- DNS checks for likely staging host candidates all failed (`api.staging.dramaflow.example`, `billing.staging.dramaflow.example`, `playback.staging.dramaflow.example`, and service-specific `*.staging.dramaflow.example` hosts)

What is still required from platform is not internal service names, but the Android-accessible external gateway/domain mapping for:
- `authBaseUrl`
- `contentBaseUrl`
- `feedBaseUrl`
- `progressBaseUrl`
- `playbackBaseUrl`
- `entitlementBaseUrl`
- `billingBaseUrl`

Drop location remains:
- `platform-intake/received/staging-external-urls/manifests/staging-external-urls.yaml`

Until those 7 external URLs are provided and verified, Android x backend integration rerun remains blocked.
