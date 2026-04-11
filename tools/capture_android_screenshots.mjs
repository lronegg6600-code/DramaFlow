import path from "node:path";
import {
  EVIDENCE_DIR,
  SCREENSHOT_DIR,
  ensureAndroidEvidenceDirs,
  getNowIso,
  getReadyDevice,
  runAdb,
  writeJson,
} from "./android_local_common.mjs";

await ensureAndroidEvidenceDirs();
const readiness = await getReadyDevice();
const outputPath = path.join(SCREENSHOT_DIR, `local-smoke-${Date.now()}.png`);
let payload;

if (!readiness.ready) {
  payload = {
    generatedAt: getNowIso(),
    captured: false,
    status: "blocked",
    reason: "No ready Android emulator/device was available for screenshot capture.",
    files: [],
  };
} else {
  const result = await runAdb(["-s", readiness.device.serial, "exec-out", "screencap", "-p"], { timeoutMs: 20000 });
  if (result.code === 0 && result.stdout) {
    await import("node:fs/promises").then((fs) => fs.writeFile(outputPath, result.stdout, "binary"));
  }
  payload = {
    generatedAt: getNowIso(),
    captured: result.code === 0,
    status: result.code === 0 ? "captured" : "failed",
    serial: readiness.device.serial,
    files: result.code === 0 ? [path.relative(process.cwd(), outputPath)] : [],
    stderr: result.stderr,
  };
}

await writeJson(path.join(EVIDENCE_DIR, "android-screenshot-index.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.captured ? 0 : 1);
