# Unblock Ops Cadence

## Daily

- 拉取所有 live issue reply 状态
- 检查 `platform-intake/received/` 是否有新输入
- 立即运行 validator
- 更新 `awaiting_reply / replied_invalid / verified / closed / escalated`
- 只要有新的 verified blocker，就重新评估 staging gate

## Second Reminder

- 对仍处于 `awaiting_reply` 的 blocker，按 due bucket 判断是否到第二轮 reminder 时间
- 如果未到期，保持 ready-to-send
- 如果到期，真实 dispatch second reminder

## Escalation

- 对超出 escalation SLA 的 blocker，触发 escalation
- escalation 后保留 issue 打开状态，但 lifecycle 进入 `escalated`

## Weekly

- 汇总本周 burn-down
- 复核哪些 staging blocker 仍未关闭
- 判断是否可以重跑真实 staging gate
