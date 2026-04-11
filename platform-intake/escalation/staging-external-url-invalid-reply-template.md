# External Staging URL Invalid Reply

The previously provided external staging URL set was rejected.

Unified rejection reason:
- hostname did not resolve

This means the values cannot be treated as Android-accessible staging endpoints.

Please provide a new manifest containing only:
- real, resolvable, Android-accessible external gateway/domain mappings for
  - `authBaseUrl`
  - `contentBaseUrl`
  - `feedBaseUrl`
  - `progressBaseUrl`
  - `playbackBaseUrl`
  - `entitlementBaseUrl`
  - `billingBaseUrl`
