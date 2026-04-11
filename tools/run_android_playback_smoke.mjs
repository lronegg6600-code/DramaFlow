import path from "node:path";
import {
  EVIDENCE_DIR,
  buildBlockedSmokePayload,
  getNowIso,
  readJsonIfExists,
  writeJson,
} from "./android_local_common.mjs";

const emulator = await readJsonIfExists(path.join(EVIDENCE_DIR, "android-emulator-readiness.json"));
const backendHealth = await readJsonIfExists(path.join(EVIDENCE_DIR, "local-backend-health-check.json"));

const blockers = [];
if (!emulator?.ready) blockers.push("android_emulator_or_device_not_ready");
if (!backendHealth?.allHealthy) blockers.push("local_backend_not_healthy");

const payload = blockers.length
  ? buildBlockedSmokePayload({
      flowId: "APP-PLAY-LOCAL-001",
      flowName: "Local App playback smoke",
      reason: "Playback App smoke could not start because emulator/device readiness is not satisfied.",
      blockers,
    })
  : {
      generatedAt: getNowIso(),
      flowId: "APP-PLAY-LOCAL-001",
      flowName: "Local App playback smoke",
      status: "manual_required",
      blockers: [],
      reason: "Prerequisites are ready. Manual player walkthrough should now be executed on emulator/device.",
    };

await writeJson(path.join(EVIDENCE_DIR, "android-playback-smoke.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.status === "pass" ? 0 : 1);
