# Blocker Milestone Plan

## staging-unblock
所有 `blocks_staging=yes` 的 blocker 都挂到这里。  
目标不是 production，而是拿到真实 staging rehearsal 入场券。

## production-readiness
只影响 production，或 staging 之后才需要继续追的 blocker 挂到这里。

## 约定
- blocker 只能在一个 milestone 下
- 默认优先 `staging-unblock`
- 进入 `verified` 仍不代表 production 可签
