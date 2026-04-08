# admin-service
# admin-service

`admin-service` 是 DramaFlow Phase 6 的后台聚合层，负责：

- Admin 登录、会话、RBAC、审计日志
- Drama / Episode 内容运营写入
- Feed 首页配置 draft / publish / rollback
- 订单 / entitlement / RTDN / playback 诊断查询
- 调用 billing-service / entitlement-service 执行补偿操作

边界：

- 内容和 feed 配置运营写入通过受限 repository 直接落库，因为当前 `content-service` / `feed-service` 仍以前台读模型为主。
- 购买 resync、entitlement recompute / grant / revoke、RTDN replay 继续调用正式 service，不在 admin-service 内复制业务规则。
Reserved for operator workflows such as catalog curation, pricing changes, publish controls, region rules, and audit views.

Phase 3 does not implement the admin surface. The service placeholder exists to keep the monorepo layout stable.
