# Platform Intake

这个目录是 DramaFlow 外部输入的唯一落点。平台、运维、仓库管理员、发布经理后续提供的真实输入，不再通过聊天记录或零散附件传递，而是统一按类别投递到这里。

## 谁放

- `repo admin / release manager`：`received/repo-identity/`
- `platform / CI / registry admin`：`received/artifact-identity/`、`received/deploy-tooling/`
- `ops / platform`：`received/cluster-access/`
- `repo admin / release manager`：`received/github-environments/`
- `platform / security / registry admin`：`received/secrets/`

## 放什么

- 每类目录只放一个主输入文件，优先命名为 `<category>.yaml`
- 可接受 `.yaml`、`.yml`、`.json`
- 不要把明文 secret 提交到仓库，secrets 类只能提供 secret ref、vault path、secret name 或截图说明

## 怎么命名

- `repo-identity/repo-identity.yaml`
- `artifact-identity/artifact-identity.yaml`
- `cluster-access/cluster-access.yaml`
- `github-environments/github-environment.yaml`
- `secrets/secret-inventory.yaml`
- `deploy-tooling/deploy-tooling.yaml`

## 工程如何验收

1. 先执行 `backend/deployments/scripts/register_received_inputs.sh`
2. 再执行 `backend/deployments/scripts/validate_received_inputs.sh`
3. 最后看：
   - `release-evidence/platform-blocker-owner-matrix.json`
   - `release-evidence/unblock-status-board.json`
   - `docs/unblock-status-board.md`

## 验收状态

- `not_received`：根本没收到
- `received_but_invalid`：收到了，但字段缺失、占位、无效
- `received_and_verified`：字段完整，可进入下一步 gate

## 失败怎么退回

- 优先引用 `platform-intake/examples/*.sample.yaml`
- 用 `docs/remaining-environment-blockers.md` 指出具体 blocker_id
- 用 `platform-intake/escalation/blocker-escalation-template.md` 留痕升级
