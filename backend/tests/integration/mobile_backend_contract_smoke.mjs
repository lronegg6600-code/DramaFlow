import { runFlow, writeReadinessAndGoNoGo } from "./_mobile_integration_helpers.mjs";

const result = await runFlow(
  "MB-SMOKE-001",
  "Android x backend contract smoke",
  "mobile-integration-smoke.json",
  [
    "authBaseUrl",
    "contentBaseUrl",
    "feedBaseUrl",
    "progressBaseUrl",
    "playbackBaseUrl",
    "entitlementBaseUrl",
    "billingBaseUrl",
  ],
);

await writeReadinessAndGoNoGo();

console.log(JSON.stringify(result, null, 2));
process.exit(result.status === "blocked" ? 1 : 0);
