# External Unblock Plan

## 目标

把当前 remaining blockers 转成外部团队可执行的任务，而不是继续停留在工程内部描述。

## 并行推进的 4 条线

1. repo admin：补 repo identity + GitHub environments
2. platform：补 artifact identity + secrets
3. ops：补 cluster access + deploy tooling
4. release manager：确定 RC 版本、审批链和升级节奏

## 依赖关系

- `repo identity` verified 后，artifact/workflow 身份才有锚点
- `cluster access + deploy tooling + secrets + GitHub staging environment` 全部 verified 后，才允许真实 staging
- `real staging` 通过后，才谈真实 canary / rollback / production

## 并行项

- repo identity 与 cluster access 可以并行
- artifact identity 与 GitHub environment 可以并行
- secrets 可以与上述所有项并行

## 检查点

- T+0：所有外部 owner 收到 bundle
- T+1：第一次 intake 校验
- T+2：未关闭 blocker 进入升级路径
