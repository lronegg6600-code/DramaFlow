# Blocker Closeout Guide

## 关闭条件

- 输入已实际收到
- 对应 validator 已通过
- 真实 issue comment 已写回 accepted/verified 结果
- evidence 已更新
- 对应 blocker 不再阻塞 staging 或 production 的目标 gate

## 不能关闭的情况

- 只有回复 comment，没有文件
- 给了文件，但 validator 没过
- 给了文件，但 acceptance criteria 不满足
- 只承诺后续补件

## reopen 条件

- 已接受输入后，后续发现文件失效
- 已关闭 blocker 后，发现提供内容与 blocker 要求不匹配
- 依赖项回退导致原 blocker 再次阻塞
