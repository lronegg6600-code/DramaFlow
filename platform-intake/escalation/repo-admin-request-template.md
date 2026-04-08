仓库管理员同学好，DramaFlow 当前卡在真实 release candidate 身份和 GitHub deploy control 未落地。请补齐并投递：

- repo-identity/repo-identity.yaml
- github-environments/github-environment.yaml

同时请确认：
- staging / production environment 已创建
- required reviewers 已配置
- prevent self-review 已开启

优先级：P0  
SLA：当天  
不提供会阻塞：真实 staging rehearsal、production 审批链  
验收命令：`backend/deployments/scripts/validate_received_inputs.sh`
