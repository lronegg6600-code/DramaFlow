import path from "node:path";
import {
  EVIDENCE_DIR,
  NETWORK_DIR,
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
      collected: false,
      status: "manual_required",
      serial: readiness.device.serial,
      outputDir: path.relative(process.cwd(), NETWORK_DIR),
      reason: "No network inspector/export pipeline is wired for unattended device collection in this repo yet.",
    }
  : {
      generatedAt: getNowIso(),
      collected: false,
      status: "blocked",
      outputDir: path.relative(process.cwd(), NETWORK_DIR),
      reason: "No ready Android emulator/device was available for network summary collection.",
    };

await writeJson(path.join(EVIDENCE_DIR, "android-network-summary.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.collected ? 0 : 1);
