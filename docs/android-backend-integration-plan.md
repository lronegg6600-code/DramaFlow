# Android x Backend Integration Plan

## Goal
Execute Android x backend real integration on staging once the full source checkout and staging environment inputs are available.

## Current Fact Base
- Current workspace is missing `.git`.
- Current workspace is missing Android source files.
- Current workspace is missing backend source files.
- Only Android build artifacts remain in `android/`.

## First Four Flows To Run Once Unblocked
1. Guest session, feed, detail, progress
2. Playback session, heartbeat, refresh, complete
3. BillingClient, purchase sync, entitlement refresh
4. Revoke, downgrade, restore, recompute

## Estimated First Pass Duration
- Environment restore and validation: 30-60 minutes
- First mobile smoke pass: 60-90 minutes
- Defect triage and go/no-go update: 30 minutes
