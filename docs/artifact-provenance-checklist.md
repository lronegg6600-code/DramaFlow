# Artifact Provenance Checklist

## Required Evidence

- [ ] commit SHA
- [ ] branch
- [ ] tag or release version
- [ ] changed files summary
- [ ] image tag per service
- [ ] image digest per service
- [ ] admin web build artifact id
- [ ] GitHub workflow run id / run url

## Why This Is Hard-Blocking

Without provenance, approvers cannot answer:

- what exact code is being released
- what exact image is deployed
- what exact build produced the artifact

That means production approval cannot be signed safely.

## Verification

- `verify_repo_identity.sh`
- `verify_artifact_identity.sh`
