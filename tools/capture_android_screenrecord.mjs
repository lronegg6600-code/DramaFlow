import path from "node:path";
import {
  EVIDENCE_DIR,
  RECORDING_DIR,
  ensureAndroidEvidenceDirs,
  getNowIso,
  getReadyDevice,
  writeJson,
} from "./android_local_common.mjs";

await ensureAndroidEvidenceDirs();
const readiness = await getReadyDevice();

const payload = readiness.ready
  ? {
      generatedAt: getNowIso(),
      captured: false,
      status: "manual_required",
      serial: readiness.device.serial,
      outputDir: path.relative(process.cwd(), RECORDING_DIR),
      reason: "Screen recording is available, but this phase did not run an unattended recording session without a deterministic UI driver.",
    }
  : {
      generatedAt: getNowIso(),
      captured: false,
      status: "blocked",
      outputDir: path.relative(process.cwd(), RECORDING_DIR),
      reason: "No ready Android emulator/device was available for screen recording.",
    };

await writeJson(path.join(EVIDENCE_DIR, "android-screenrecord-index.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.status === "captured" ? 0 : 1);
