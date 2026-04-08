# Blocker Ticket Launch Runbook

## 当前状态
- 41 个 blocker issue 已真实创建
- 41 个 blocker issue 已真实指派
- 41 个 blocker issue 已发出首轮 reminder
- 当前主生命周期：`awaiting_reply`

## real launch 步骤
1. `build_blocker_issue_payloads.mjs`
2. `seed_blocker_labels.mjs`
3. `seed_blocker_milestones.mjs`
4. `seed_blocker_project_config.mjs`
5. `launch_blocker_tickets.mjs`
6. `sync_live_issue_metadata.mjs`
7. `assign_blocker_owners.mjs`
8. `dispatch_first_reminders.mjs`
9. `publish_launch_outcome.mjs`

## 后续操作
- 收到输入后跑 validator
- 无效回复：`comment_on_invalid_reply.mjs`
- 验证通过：`comment_on_blocker_verified.mjs`
- 后续失效：`comment_on_blocker_reopened.mjs`
