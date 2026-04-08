# Trace Walkthrough: Admin Mutation -> Audit Log

## Goal

Dangerous admin operations are only safe to ship when the mutation and the audit trail are both visible. This walkthrough explains what to confirm.

## Expected Span Order

1. `admin-web` proxy route receives operator action
2. `admin-service` receives admin mutation request
3. RBAC check span
4. service mutation span
5. repository write span
6. audit write span
7. admin audit query later returns the new record

## Required Context

- `trace_id`
- `request_id`
- `admin_user_id`
- `action`
- resource identifiers such as `purchase_token`, `session_id`, `drama_id`, `episode_id`

## Healthy Result

- Unauthorized users are blocked before mutation.
- Authorized mutation succeeds or fails with a clear error code.
- Audit log exists for both success and failure paths where policy requires it.

## Broken Result

- Mutation succeeds without audit record.
- Audit exists but lacks actor or resource identifiers.
- RBAC failure happens too late, after side effects already started.

## Immediate Triage

1. Query the audit log page or API for the last action.
2. Confirm `admin_user_id`, `action`, and target resource are present.
3. If missing, freeze dangerous admin operations and treat as release blocker.
