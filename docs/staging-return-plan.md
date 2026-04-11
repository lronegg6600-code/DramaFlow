# Staging Return Plan

## Why Attention Can Move Back To Staging
The local line is no longer the active blocker:
- local backend is healthy
- Android physical-device smoke passed
- entitlement contract is fixed in source
- live localhost runtime has been replaced
- post-cutover device regression passed

## Remaining Staging Scope
- Obtain 7 real resolvable Android-accessible external staging URLs
- Validate DNS / format / reachability / path
- Export verified Android staging env
- Rerun Android x backend staging integration

## Current Core Blocker
Platform has still not provided the 7 real external staging URLs on issue `#42`.

## Next Shortest Path
Resume the external staging URL acceptance workflow and keep the blocker pinned to platform environment input, not local code or local runtime.
