# Issue Operations Runbook

## 当前真实状态
- 41 个 blocker issue 已创建
- 41 个 blocker issue 已指派
- 41 个 blocker issue 已发出首轮 reminder
- 当前生命周期主状态：`awaiting_reply`

## 操作顺序
1. 外部团队在 issue 中回复，并把输入投递到 `platform-intake/received/`
2. 工程运行 validator
3. 如输入无效：
   - 运行 `comment_on_invalid_reply.mjs`
   - 状态推进到 `replied_invalid`
4. 如输入验证通过：
   - 运行 `comment_on_blocker_verified.mjs`
   - 状态推进到 `verified`
5. blocker 真解除后：
   - 更新 closeout evidence
   - 状态推进到 `closed`
