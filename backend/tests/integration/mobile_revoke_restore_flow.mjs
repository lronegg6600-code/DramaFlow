import { runFlow } from "./_mobile_integration_helpers.mjs";

const result = await runFlow(
  "MB-REVOKE-001",
  "Revoke, downgrade, restore, recompute flow",
  "mobile-revoke-restore-flow.json",
  ["entitlementBaseUrl", "playbackBaseUrl", "billingBaseUrl"],
);

console.log(JSON.stringify(result, null, 2));
process.exit(result.status === "blocked" ? 1 : 0);
