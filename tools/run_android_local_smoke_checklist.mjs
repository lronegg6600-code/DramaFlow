import path from "node:path";
import {
  EVIDENCE_DIR,
  buildBlockedSmokePayload,
  getNowIso,
  readJsonIfExists,
  writeJson,
} from "./android_local_common.mjs";

const emulator = await readJsonIfExists(path.join(EVIDENCE_DIR, "android-emulator-readiness.json"));
const target = await readJsonIfExists(path.join(EVIDENCE_DIR, "android-debug-target-check.json"));
const backendHealth = await readJsonIfExists(path.join(EVIDENCE_DIR, "local-backend-health-check.json"));

const blockers = [];
if (!emulator?.ready) blockers.push("android_emulator_or_device_not_ready");
if (!target?.ready) blockers.push("android_debug_target_not_ready");
if (!backendHealth?.allHealthy) blockers.push("local_backend_not_healthy");

const payload = blockers.length
  ? buildBlockedSmokePayload({
      flowId: "APP-AFD-LOCAL-001",
      flowName: "Local App Feed / Detail smoke",
      reason: "App-level local smoke could not start because emulator/device readiness is not satisfied.",
      blockers,
    })
  : {
      generatedAt: getNowIso(),
      flowId: "APP-AFD-LOCAL-001",
      flowName: "Local App Feed / Detail smoke",
      status: "manual_required",
      blockers: [],
      reason: "Prerequisites are ready. Manual app walkthrough should now be executed on emulator/device.",
    };

await writeJson(path.join(EVIDENCE_DIR, "android-feed-detail-smoke.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.status === "pass" ? 0 : 1);
