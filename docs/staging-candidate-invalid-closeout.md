# Staging Candidate Invalid Closeout

## Closeout Result
- first-round candidate manifest: `platform-intake/received/staging-external-urls/manifests/staging-external-urls.candidate.yaml`
- candidate env probe file: `.env.staging.mobile.candidate`
- candidate closeout completed: `yes`

## Unified Rejection Reason
- all 7 candidate hostnames failed DNS resolution

## Lifecycle Outcome
- candidate lifecycle state: `candidate_received_but_invalid`
- next lifecycle state: `real_url_not_received`

## Carry Forward Blocker
- platform must provide real, resolvable, Android-accessible external gateway/domain mapping for all 7 services
