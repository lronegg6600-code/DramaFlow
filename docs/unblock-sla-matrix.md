# Unblock SLA Matrix

| 类别 | 默认优先级 | 默认 SLA | 未按时交付影响 | 升级路径 |
| --- | --- | --- | --- | --- |
| Repository identity | P0/P1 | same day | 真实 RC 无法建立 | release manager -> repo admin -> engineering manager |
| Artifact identity | P0/P1 | same day | 不能证明发版产物身份 | platform lead -> registry admin -> release manager |
| Cluster access | P0 | same day | 无法真实 staging / rollback | ops lead -> platform lead -> engineering manager |
| Deployment tooling | P0 | same day | 无法 helm deploy | ops lead -> platform lead |
| GitHub release control | P0/P1 | same day | 无法真实 approval / deploy gate | release manager -> repo admin -> org admin |
| Secrets / config | P0 | same day | 主链路无法在真实环境起服 | security -> platform lead -> engineering manager |
