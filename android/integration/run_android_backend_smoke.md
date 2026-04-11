# Run Android x Backend Smoke

1. Restore the full Android source checkout.
2. Restore the backend source checkout.
3. Provide staging base URLs:
   - `DRAMAFLOW_AUTH_BASE_URL`
   - `DRAMAFLOW_CONTENT_BASE_URL`
   - `DRAMAFLOW_FEED_BASE_URL`
   - `DRAMAFLOW_PROGRESS_BASE_URL`
   - `DRAMAFLOW_PLAYBACK_BASE_URL`
   - `DRAMAFLOW_ENTITLEMENT_BASE_URL`
   - `DRAMAFLOW_BILLING_BASE_URL`
4. Run:

```powershell
node Z:\Projects\DramaFlow\backend\tests\integration\mobile_backend_contract_smoke.mjs
node Z:\Projects\DramaFlow\backend\tests\integration\mobile_auth_feed_detail_flow.mjs
node Z:\Projects\DramaFlow\backend\tests\integration\mobile_playback_session_flow.mjs
node Z:\Projects\DramaFlow\backend\tests\integration\mobile_billing_entitlement_flow.mjs
node Z:\Projects\DramaFlow\backend\tests\integration\mobile_revoke_restore_flow.mjs
```

5. Capture Android logs, backend logs, and issue any blocker found in the mobile defect matrix.
