# Staging Real External URL Derivation Request

## Why This Follow-up Is More Specific
- repo/source recovery is complete
- internal service map is already confirmed
- first-round repo-derived candidate external URLs were rejected because `hostname did not resolve`
- this workstation has `kubectl`, but no active kube context
- repo examples show staging access is expected to come from a `platform-managed-secret`

## What Platform Must Provide Now
Provide **either** of the following:

### Option A: Final Android-accessible external URLs
- `authBaseUrl`
- `contentBaseUrl`
- `feedBaseUrl`
- `progressBaseUrl`
- `playbackBaseUrl`
- `entitlementBaseUrl`
- `billingBaseUrl`

Requirements:
- full `http://` or `https://` URLs
- DNS-resolvable
- Android-accessible
- mapped to the correct staging service route

### Option B: Inputs that allow engineering to derive them directly
- staging kubeconfig or kube context for `dramaflow-staging`
- ingress / gateway / external host mapping for the 7 services
- any service-to-public-route mapping currently held in platform-managed secret/config

If Option B is provided, engineering will derive and verify the 7 URLs directly and proceed with Android env export plus mobile rerun.

## Current Blocking Fact
- `backend/deployments/kubeconfig/staging.context.example` shows:
  - `context_name=dramaflow-staging`
  - `kubeconfig_source=platform-managed-secret`
- current workstation has no kube contexts configured
- no ingress/gateway host mapping exists in repo checkout

## Impact If Neither Option Is Provided
- verified Android staging env cannot be exported
- Android x backend integration rerun remains blocked
- mobile staging rehearsal cannot advance
