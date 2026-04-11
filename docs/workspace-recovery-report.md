# Workspace Recovery Report

## Workspace Classification
- Current workspace type: `residual export / partial working copy`
- Git-backed checkout: `no`
- Android source tree: `missing`
- Backend source tree: `partial`

## Audit Conclusion
This is not a source-complete DramaFlow repo. It contains integration artifacts and Android build outputs, but not the Android or backend source-of-truth trees required for real integration work.

## Concrete Audit Facts
- `.git` root candidates found: `0`
- Android source files found: `0`
- Android Gradle settings/build files found: `0`
- Backend source files found in current workspace: `6`
- Backend service tree `backend/services/`: `missing`
