# Release Manager Ticket Body

需要发布经理确认 RC 身份、reviewer 链、bootstrap 密码托管位。

- 需要提供：RC version / reviewer policy / admin bootstrap password ref
- 放置位置：`platform-intake/received/repo-identity/` 或 `platform-intake/received/github-environments/`
- 验收命令：`backend/deployments/scripts/validate_received_inputs.sh`
- 截止时间：same_day
- 不提供会阻塞：真实 staging、后续 production 审批准备

群聊简版：
DramaFlow 缺 RC 版本和 reviewer 链确认，请今天内补齐，否则真实 staging 不能启动。
