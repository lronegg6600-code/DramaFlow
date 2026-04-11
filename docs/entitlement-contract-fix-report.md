# Entitlement Contract Fix Report

## Decision
- Fix landing: `both`
- Primary goal: eliminate the physical-device restore crash caused by `entitlements = null`

## Backend Fix
- File: [service.go](/Z:/Projects/DramaFlow/backend/services/entitlement-service/internal/service/service.go)
- `Me()` now normalizes nil entitlements to an empty slice before serializing the response.
- File: [openapi.yaml](/Z:/Projects/DramaFlow/backend/services/entitlement-service/openapi/openapi.yaml)
- `/v1/entitlements/me` now documents `entitlements` as a required array.

## Android Fix
- File: [EntitlementDtos.kt](/Z:/Projects/DramaFlow/android/core/network/src/main/java/com/dramaflow/core/network/dto/EntitlementDtos.kt)
- `entitlements` is now nullable in the DTO and normalized through `entitlementsOrEmpty()`.
- File: [EntitlementRemoteDataSource.kt](/Z:/Projects/DramaFlow/android/core/network/src/main/java/com/dramaflow/core/network/source/EntitlementRemoteDataSource.kt)
- Domain mapping now routes through `toDomain()`, which safely produces `emptyList()` for null entitlements.

## Verification
- Android unit test: `pass`
- Debug APK rebuild: `pass`
- Physical-device retest: `pass`

## Important Runtime Nuance
- The repo source is fixed on both sides.
- The currently running localhost entitlement-service process still returns `entitlements: null`.
- This means Android-side tolerance is what removed the device crash in the current local runtime.
- Backend runtime verification will require rebuilding/restarting the local entitlement-service instance.
