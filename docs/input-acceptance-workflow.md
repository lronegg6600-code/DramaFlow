# Input Acceptance Workflow

## 流程

1. 外部 owner 回复
2. 工程把输入落到 `platform-intake/received/`
3. 跑对应 validator
4. validator 输出写到：
   - `platform-intake/validation-results/`
   - `release-evidence/`
5. 运行 close 或 reopen 脚本
6. 更新 `unblock-status-board`

## 判定人

engineering release owner 负责 accepted / rejected 的最终判定。

## 关键原则

不要只看“给了文件”，要看 validator 是否返回 `received_and_verified`。
