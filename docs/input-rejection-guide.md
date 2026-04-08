# Input Rejection Guide

## 何时拒收

- 文件放错目录
- 关键字段缺失
- 仍是 placeholder
- 给了截图但无法映射到 blocker 所需输入
- validator 结果为 `received_but_invalid`

## 拒收后动作

1. 运行 `node backend/tests/release/reopen_blocker_from_invalid_input.mjs`
2. 在 `release-evidence/input-rejection-log.json` 留痕
3. 用 `.github/ISSUE_TEMPLATE/blocker-invalid-reply.yml` 或对应模板回填补件请求
