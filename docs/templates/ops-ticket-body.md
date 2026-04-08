# Ops Ticket Body

需要运维提供 cluster access 与 deploy tooling 输入。

- 需要提供：`cluster-access / deploy-tooling`
- 放置位置：`platform-intake/received/<category>/`
- 验收命令：`backend/deployments/scripts/validate_received_inputs.sh`
- 截止时间：same_day
- 不提供会阻塞：真实 staging deploy / rollback

群聊简版：
DramaFlow 缺 staging kubeconfig/context/RBAC/helm，请今天内投递 cluster-access 和 deploy-tooling 输入包。
