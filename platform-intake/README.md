# Platform Intake

This directory is the single drop point for external DramaFlow inputs.

## Providers
- `repo admin / release manager` -> `received/repo-identity/`
- `platform / CI / registry admin` -> `received/artifact-identity/`, `received/deploy-tooling/`
- `ops / platform` -> `received/cluster-access/`
- `repo admin / release manager` -> `received/github-environments/`
- `platform / security / registry admin` -> `received/secrets/`
- `repo admin / android owner / backend owner / platform` -> `received/source-recovery/`
- `platform` -> `received/staging-inputs/`
- `platform` -> `received/staging-external-urls/`

## Staging Input Package
The current Android x backend rerun is blocked by 7 staging URLs:
- `authBaseUrl`
- `contentBaseUrl`
- `feedBaseUrl`
- `progressBaseUrl`
- `playbackBaseUrl`
- `entitlementBaseUrl`
- `billingBaseUrl`

Drop the package at:
- `platform-intake/received/staging-inputs/manifests/staging-base-urls.yaml`

Use:
- `platform-intake/examples/staging-base-urls.sample.yaml`
- `platform-intake/examples/staging-base-urls-manifest.sample.yaml`

## Validation
Run:
1. `node tools/ingest_staging_inputs.mjs`
2. `node tools/validate_staging_inputs.mjs`
3. `node tools/export_staging_env_file.mjs`
4. `node tools/rerun_mobile_with_staging_inputs.mjs`

Evidence:
- `release-evidence/staging-input-validation.json`
- `release-evidence/staging-env-export.json`
- `release-evidence/mobile-rerun-with-staging-inputs.json`

## External Android-Accessible Staging URL Package
The repo also contains an internal-only service map, but Android rerun requires external gateway/domain mapping.

Drop the external package at:
- `platform-intake/received/staging-external-urls/manifests/staging-external-urls.yaml`

Use:
- `platform-intake/examples/staging-base-urls-external.sample.yaml`
- `platform-intake/examples/staging-gateway-mapping.sample.yaml`

Validation:
1. `node tools/ingest_external_staging_urls.mjs`
2. `node tools/validate_external_staging_urls.mjs`
3. `node tools/export_android_staging_env.mjs`
4. `node tools/rerun_mobile_after_external_urls.mjs`

Important:
- internal URLs such as `http://auth-service:8081` are not accepted as Android external URLs

## Statuses
- `not_received`
- `received_but_invalid`
- `verified`
- `closed`
- `escalated`
