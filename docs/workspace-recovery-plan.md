# Workspace Recovery Plan

## Goal
Restore DramaFlow to a git-backed, source-complete workspace that can rerun Android x backend integration.

## Current Findings
- `.git` is missing in the current workspace.
- Android source modules are missing; only build outputs remain.
- Backend service source tree is missing.
- No alternate source-of-truth candidate was found under the current scan scope.

## Recovery Order
1. Restore a git-backed checkout
2. Restore Android source tree
3. Restore backend service source tree
4. Restore docs, release-evidence, and platform-intake alongside the source tree
5. Inject staging base URLs
6. Rerun mobile readiness and the four mobile smoke flows
