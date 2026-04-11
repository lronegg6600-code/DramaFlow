# External Source Recovery Request

## Request
Provide the authoritative DramaFlow source checkout or archive that contains:
- `.git`
- `android/` source modules
- `backend/services/` source tree
- `docs/`
- `release-evidence/`
- `platform-intake/`

## Why
Without these directories, Android x backend real integration cannot start.

## After Delivery
Run:
- `node Z:\Projects\DramaFlow\tools\audit_workspace_layout.mjs`
- `node Z:\Projects\DramaFlow\tools\verify_android_source_tree.mjs`
- `node Z:\Projects\DramaFlow\tools\verify_backend_source_tree.mjs`
- `node Z:\Projects\DramaFlow\tools\rerun_mobile_readiness_if_recovered.mjs`
