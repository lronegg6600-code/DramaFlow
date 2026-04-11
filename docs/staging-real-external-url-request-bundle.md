# Real External Staging URL Request Bundle

## Why The Previous Candidate Was Rejected
- first-round repo-derived candidate URLs were received
- all 7 were rejected with the same reason: `hostname did not resolve`
- candidate values therefore cannot be treated as Android-accessible staging endpoints

## What Is Required Now
Provide a second-round manifest containing only real, resolvable, Android-accessible external gateway/domain mappings for:
- `authBaseUrl`
- `contentBaseUrl`
- `feedBaseUrl`
- `progressBaseUrl`
- `playbackBaseUrl`
- `entitlementBaseUrl`
- `billingBaseUrl`

## Format Requirements
- must be full `http://` or `https://` URLs
- must resolve in DNS
- must be reachable from Android client environment
- must point to staging, not dev or prod
- must map to the correct service route
- candidate names such as `https://api.staging.dramaflow.example/...` are not acceptable unless they actually resolve

## Delivery Location
- `platform-intake/received/staging-external-urls/responses/staging-external-urls.yaml`

## Issue Tracking
- issue: `#42`
- current state: first-round candidate invalid closeout completed, second-round request open, second-round reminder sent

## Validation Steps
1. `node tools/ingest_real_external_urls.mjs`
2. `node tools/validate_real_external_urls.mjs`
3. `node tools/export_verified_android_staging_env.mjs`
4. `node tools/rerun_mobile_with_verified_external_urls.mjs`

## Impact If Not Provided
- Android x backend integration remains blocked
- verified Android staging env cannot be exported
- mobile defect burn-down cannot move from environment blocking to runtime defect triage
- next real staging rehearsal evaluation cannot start from the mobile side

## Ready-To-Send Message (Work Chat Short Version)
平台同学帮忙今天补齐 staging external URLs。当前唯一卡点有两个：  
1) 执行环境需要可用 `gh` + token（脚本已支持，工程侧可马上跑）；  
2) 你们还没提交 7 条真实 external staging URL（auth/content/feed/progress/playback/entitlement/billing）。  
脚本链路已经 ready：URL 一旦给齐，我们同轮可完成 reply 抓取、URL 校验、burndown 更新，并切入 Android × backend staging integration。  
投递路径：`platform-intake/received/staging-external-urls/responses/staging-external-urls.yaml`。

## Ready-To-Send Message (Issue / Comment Formal Version)
External URL unblock update for issue `#42`:

- Current blocker is now narrowed to two execution prerequisites:
  1. Runtime environment must have usable `gh` and token (`GH_TOKEN` or `GITHUB_TOKEN`) for automated fetch/comment dispatch.
  2. Platform has not submitted the 7 real external staging URLs yet.
- Tooling and validation scripts are already ready on engineering side.
- Once all 7 URLs are submitted in the required manifest path, we will execute in the same cycle:
  1. `node tools/fetch_real_external_url_replies.mjs`
  2. `node tools/validate_real_external_urls.mjs`
  3. `node tools/summarize_real_external_url_burndown.mjs`
- If all URLs pass validation (7/7 verified), we immediately move into Android × backend staging integration.
