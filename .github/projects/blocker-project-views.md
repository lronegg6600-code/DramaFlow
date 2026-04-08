# Blocker Project Views

## Daily Unblock
- Group by `Status`
- Filter: `Blocks Staging = yes`
- Sort: `Due Bucket`, then `SLA Level`
- Used by: daily unblock standup

## Provider Queue
- Group by `Provider Role`
- Filter: `Status != Closed`
- Used by: owner follow-up and cross-team coordination

## Invalid Replies
- Filter: `Status = Replied Invalid`
- Used by: rejection handling and reopen review

## Weekly Escalation
- Filter: `Status = Escalated`
- Used by: weekly escalation review and release manager summary

## Production Residuals
- Filter: `Blocks Production = yes`
- Used by: post-staging production readiness review
