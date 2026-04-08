# Blocker Escalation Policy

## 触发条件

- P0 blocker 超过 1 个工作日仍 `not_received`
- 任一 blocker 连续两次 `received_but_invalid`
- 依赖链上 blocker 导致真实 staging 无法启动

## 升级动作

1. 在工单或群消息中引用 blocker_id
2. 附上 sample/template 路径
3. 附上验收命令
4. 指明如果今日不闭环，会继续保持 `Still blocked for real staging execution`

## 留痕要求

- 所有升级都要在 `platform-intake/escalation/` 模板基础上发出
- 升级后同步更新 `docs/unblock-status-board.md`
