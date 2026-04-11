import { runFlow } from "./_mobile_integration_helpers.mjs";

const result = await runFlow(
  "MB-PLAY-001",
  "Playback session, heartbeat, refresh, complete flow",
  "mobile-playback-flow.json",
  ["playbackBaseUrl", "entitlementBaseUrl"],
);

console.log(JSON.stringify(result, null, 2));
process.exit(result.status === "blocked" ? 1 : 0);
