## Candidate external URL probe result

Engineering derived the most plausible external URL candidates from existing repo templates and Android staging examples:

- `https://api.staging.dramaflow.example/auth`
- `https://api.staging.dramaflow.example/content`
- `https://api.staging.dramaflow.example/feed`
- `https://api.staging.dramaflow.example/progress`
- `https://api.staging.dramaflow.example/playback`
- `https://api.staging.dramaflow.example/entitlement`
- `https://api.staging.dramaflow.example/billing`

Validation result:
- all 7 were received as candidate inputs
- all 7 were rejected as `received_but_invalid`
- reason: hostname did not resolve in current environment

Probe rerun result:
- contract smoke: blocked
- auth/feed/detail: blocked
- playback: blocked
- billing/entitlement: blocked
- revoke/restore: blocked

Current blocker is now more specific:
- external URL values are no longer simply missing
- repo-derived candidates exist but are not resolvable
- platform still needs to provide the actual Android-accessible staging gateway/domain mapping
