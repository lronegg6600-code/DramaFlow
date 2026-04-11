# Source Recovery Status Transition Guide

## States
- `not_received`: no recovery payload received
- `awaiting_reply`: reminder sent and waiting for owner response
- `received_partial`: payload received but incomplete
- `received_but_invalid`: payload received but invalid or unusable
- `verified`: payload passed validation
- `closed`: blocker closed after verified acceptance
- `reopened`: previously closed blocker became invalid again
- `escalated`: owner has exceeded the defined escalation rule

## Rules
- `not_received -> awaiting_reply`: only after a live reminder is actually dispatched
- `awaiting_reply -> received_partial`: some required files received, but acceptance not complete
- `awaiting_reply -> received_but_invalid`: payload received but does not satisfy validation
- `awaiting_reply -> verified`: payload satisfies validation
- `verified -> closed`: acceptance completed and blocker explicitly closed
- `closed -> reopened`: later validation failure or invalidated source payload
- `awaiting_reply -> escalated`: SLA breach confirmed and escalation dispatched

## Current Cycle
No live reminder or reply dispatch happened in this environment, so all source recovery blockers remain `not_received`.
