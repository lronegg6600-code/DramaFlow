# Repo Admin Request Bundle

## 背景

DramaFlow 当前仍无法证明真实 release candidate 身份，也无法验证 GitHub deploy control。

## 需要仓库管理员提供的输入

1. `platform-intake/received/repo-identity/repo-identity.yaml`
2. `platform-intake/received/github-environments/github-environment.yaml`

## 必须确认的配置

- `.git` 对应真实 checkout
- commit SHA / branch / RC tag 可追溯
- staging / production environments 已创建
- required reviewers 已配置
- prevent self-review 已开启
- deployment branches / tags policy 已设置

## 示例

- [repo-identity.sample.yaml](/Z:/Projects/DramaFlow/platform-intake/examples/repo-identity.sample.yaml)
- [github-environment.sample.yaml](/Z:/Projects/DramaFlow/platform-intake/examples/github-environment.sample.yaml)

## 验证方式

```bash
backend/deployments/scripts/validate_received_inputs.sh
```

## 不提供会阻塞什么

- 真实 staging 触发
- release candidate 身份确认
- production 审批链
