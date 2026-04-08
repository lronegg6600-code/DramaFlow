# Blocker Label Taxonomy

- `blocker`: 所有 release blocker issue 的基础标签
- `blocker:repo-identity`
- `blocker:artifact-identity`
- `blocker:cluster-access`
- `blocker:deploy-tooling`
- `blocker:github-env`
- `blocker:secrets`
- `blocker:staging`: 会阻塞真实 staging
- `blocker:production`: 会阻塞 production
- `blocker:external`: 需要外部团队输入
- `blocker:waiting`: 已发出，等待回复
- `blocker:invalid-reply`: 回复无效待补件
- `blocker:verified`: 工程已验证输入有效
- `blocker:closed`: blocker 已关闭

## 动态运行标签

- `severity:p0` / `severity:p1`
- `owner:repo-admin` / `owner:ops` / `owner:release-manager`
- `provider:repo-owner` / `provider:platform` / `provider:platform-security` 等
- `sla:same-day` / `sla:next-business-day` / `sla:two-business-days`

这些标签由 seed 脚本从 owner matrix 自动生成，避免真实 launch 时出现“payload 用了标签，但仓库里没 seed”的失败。
