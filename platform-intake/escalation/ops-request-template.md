运维同学好，DramaFlow 当前卡在真实 staging 集群访问与部署控制面。请补齐并投递：

- cluster-access/cluster-access.yaml
- deploy-tooling/deploy-tooling.yaml

需要至少包含：
- kubeconfig 分发路径
- current-context / staging context
- namespace
- RBAC role
- `can_get_deployments=true`
- `can_patch_deployments=true`
- helm 版本与 overlay 路径

优先级：P0  
SLA：当天  
不提供会阻塞：真实 staging deploy / rollback  
验收命令：`backend/deployments/scripts/validate_received_inputs.sh`
