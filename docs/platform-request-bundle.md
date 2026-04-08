# Platform Request Bundle

## 背景

DramaFlow 当前业务主链路已完成工程侧收口，仍然无法进入真实 staging，阻塞点集中在平台输入未到位。

## 当前状态

- Ready for platform handoff execution: yes
- Ready for real staging execution: no
- 当前 blocker 主要集中在 artifact、deploy tooling、secrets

## 需要平台提供的输入

1. `platform-intake/received/artifact-identity/artifact-identity.yaml`
2. `platform-intake/received/deploy-tooling/deploy-tooling.yaml`
3. `platform-intake/received/secrets/secret-inventory.yaml`

## 输入格式示例

- [artifact-identity.sample.yaml](/Z:/Projects/DramaFlow/platform-intake/examples/artifact-identity.sample.yaml)
- [deploy-tooling.sample.yaml](/Z:/Projects/DramaFlow/platform-intake/examples/deploy-tooling.sample.yaml)
- [secret-inventory.sample.yaml](/Z:/Projects/DramaFlow/platform-intake/examples/secret-inventory.sample.yaml)

## 提供后如何验证

执行：

```bash
backend/deployments/scripts/register_received_inputs.sh
backend/deployments/scripts/validate_received_inputs.sh
```

## SLA

- P0：当天
- 如 2 个工作日内未提供，进入升级路径

## 不提供会阻塞什么

- 真实 staging rehearsal
- 后续真实 canary / rollback
- production 审批申请

## 升级路径

platform owner -> ops lead -> release manager -> engineering manager
