# Blocker Ticket Playbook

## 当前 Phase 16 状态

- awaiting_reply: 41
- replied_invalid: 0
- verified: 0
- closed: 0
- escalated: 0

## lifecycle 定义

- `created_unassigned`: issue 已创建，但还没有绑定 owner
- `assigned`: owner 已绑定，但尚未发出首轮提醒
- `awaiting_reply`: 已发出提醒，等待外部回复或输入投递
- `replied_invalid`: 收到回复或文件，但 validator 不通过，需要补件
- `verified`: validator 通过，等待执行 close
- `closed`: blocker 已解除
- `escalated`: 超过 SLA 未回复，已进入升级路径

## Phase 16 处理规则

1. `awaiting_reply -> replied_invalid`
   条件：收到输入，但 validator 不通过。
2. `awaiting_reply -> verified`
   条件：收到输入，且 validator 通过。
3. `verified -> closed`
   条件：issue comment 已写回，evidence 已落盘，blocker 不再阻塞对应 gate。
4. `closed -> reopened`
   条件：已接受输入后又发现失效、缺项或不匹配 acceptance criteria。
5. `awaiting_reply -> escalated`
   条件：达到 escalation SLA，且仍无有效回复。
6. `replied_invalid -> awaiting_reply`
   条件：补件请求已发出，继续等待新的有效输入。
