# Blocker Reply Handling Guide

## Phase 16 当前事实

- 真实回复数：0
- 新输入文件数：0
- 当前没有任何 blocker 进入 `verified` 或 `closed`

## 处理流程

1. 拉取 GitHub issue comments，过滤掉机器人自身 comment。
2. 扫描 `platform-intake/received/` 中是否有新增输入文件。
3. 如果没有输入：
   - 保持 `awaiting_reply`
   - 根据 SLA 准备 second reminder 或 escalation
4. 如果有输入：
   - 跑对应 validator
   - 通过则进入 `verified`
   - 不通过则进入 `replied_invalid`
5. 把处理结果回写到 issue comment、evidence、status board。

## 接受与拒收原则

- 只认 validator 结果，不认口头承诺
- 文件存在但格式不对，算 invalid
- 文件格式对但关键字段缺失，算 invalid
- 只有 comment 没有输入文件，不算 accepted
