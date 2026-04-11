import { runFlow } from "./_mobile_integration_helpers.mjs";

const result = await runFlow(
  "MB-AFD-001",
  "Guest session, feed, detail, episodes, progress flow",
  "mobile-auth-feed-detail-flow.json",
  ["authBaseUrl", "feedBaseUrl", "contentBaseUrl", "progressBaseUrl"],
);

console.log(JSON.stringify(result, null, 2));
process.exit(result.status === "blocked" ? 1 : 0);
