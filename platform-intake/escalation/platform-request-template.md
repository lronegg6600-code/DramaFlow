平台同学好，DramaFlow 已完成工程侧准备，当前卡在真实 staging 前置输入未到位。请按 `platform-intake/examples/` 中对应 sample 提供以下输入，并投递到 `platform-intake/received/`：

- artifact-identity/artifact-identity.yaml
- deploy-tooling/deploy-tooling.yaml
- secrets/secret-inventory.yaml

优先级：P0  
SLA：当天  
不提供会阻塞：真实 staging rehearsal、后续 canary/rollback drill  
验收命令：`backend/deployments/scripts/validate_received_inputs.sh`
