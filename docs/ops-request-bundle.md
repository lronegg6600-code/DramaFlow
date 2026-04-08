# Ops Request Bundle

## 背景

DramaFlow 当前没有真实 staging 集群访问与 rollback 控制面，导致真实 rehearsal 无法开始。

## 需要运维提供的输入

1. `platform-intake/received/cluster-access/cluster-access.yaml`
2. `platform-intake/received/deploy-tooling/deploy-tooling.yaml`

## 需要明确的最小动作

- 提供 kubeconfig 分发方式
- 配置 current-context / staging context
- 指定 namespace
- 提供 deploy service account / RBAC role
- 确认 `kubectl auth can-i get deployments`
- 确认 `kubectl auth can-i patch deployments`
- 提供 helm 版本和实际 overlay 文件

## 示例

- [cluster-access.sample.yaml](/Z:/Projects/DramaFlow/platform-intake/examples/cluster-access.sample.yaml)
- [deploy-tooling.sample.yaml](/Z:/Projects/DramaFlow/platform-intake/examples/deploy-tooling.sample.yaml)

## SLA

- P0：当天

## 不提供会阻塞什么

- 真实 staging deploy
- canary / rollback 演练
