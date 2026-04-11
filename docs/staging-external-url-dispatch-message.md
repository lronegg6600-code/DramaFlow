# Staging External URL Dispatch Message

请 platform 立即提供 DramaFlow Android × backend 联调所需的 7 个 **Android 可访问** external staging URLs。

当前状态：
- repo/source 已恢复
- internal cluster service map 已确认
- Android 联调当前唯一主阻塞是 external staging gateway/domain mapping 缺失

必填字段：
- `authBaseUrl`
- `contentBaseUrl`
- `feedBaseUrl`
- `progressBaseUrl`
- `playbackBaseUrl`
- `entitlementBaseUrl`
- `billingBaseUrl`

格式要求：
- 必须是完整的 `http://` 或 `https://` URL
- 必须可供 Android 客户端直接访问
- 必须指向 staging，不是 dev / prod
- `http://auth-service:8081` 这类 internal service URL 不可接受

投递位置：
- `platform-intake/received/staging-external-urls/manifests/staging-external-urls.yaml`

样例：
- `platform-intake/examples/staging-base-urls-external.sample.yaml`

提供后工程侧将立即执行：
1. `node tools/ingest_platform_staging_urls.mjs`
2. `node tools/validate_platform_staging_urls.mjs`
3. `node tools/export_mobile_staging_env.mjs`
4. `node tools/rerun_mobile_integration_with_external_urls.mjs`

不提供的影响：
- Android × backend integration rerun 持续 blocked
- mobile defect burn-down 无法开始
- 下一轮真实 staging rehearsal 无法进入移动端评估
