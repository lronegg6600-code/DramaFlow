import { runFlow } from "./_mobile_integration_helpers.mjs";

const result = await runFlow(
  "MB-BILL-001",
  "BillingClient, purchase sync, entitlement refresh flow",
  "mobile-billing-entitlement-flow.json",
  ["billingBaseUrl", "entitlementBaseUrl"],
);

console.log(JSON.stringify(result, null, 2));
process.exit(result.status === "blocked" ? 1 : 0);
