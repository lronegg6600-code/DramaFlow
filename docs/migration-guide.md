# Migration Guide

## Naming

- forward: `NNN_feature_name.sql`
- rollback: `NNN_feature_name_rollback.sql`

## Rules

- every non-baseline migration must have rollback pair
- destructive DDL must include backup / snapshot note
- release PR must explain impact on billing / entitlement / playback if affected

## Commands

- `make -C backend migrate-up`
- `make -C backend migrate-down`
- `make -C backend migrate-status`
- `make -C backend migrate-check`
