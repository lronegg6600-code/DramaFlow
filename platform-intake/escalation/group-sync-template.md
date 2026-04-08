DramaFlow 当前不是代码阻塞，而是外部输入未到位。请相关 owner 今天内把对应输入包投递到 `platform-intake/received/`：

- repo admin：repo-identity + github-environments
- platform：artifact-identity + secrets
- ops：cluster-access + deploy-tooling
- release manager：RC version / reviewer 确认

未补齐将继续保持 `Still blocked for real staging execution`。验收命令统一使用：`backend/deployments/scripts/validate_received_inputs.sh`
