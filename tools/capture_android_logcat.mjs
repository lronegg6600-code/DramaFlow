import path from "node:path";
import {
  EVIDENCE_DIR,
  LOGCAT_DIR,
  ensureAndroidEvidenceDirs,
  getNowIso,
  getReadyDevice,
  runAdb,
  writeJson,
  writeText,
} from "./android_local_common.mjs";

await ensureAndroidEvidenceDirs();
const readiness = await getReadyDevice();
const logPath = path.join(LOGCAT_DIR, `local-smoke-${Date.now()}.log`);
let payload;

if (!readiness.ready) {
  payload = {
    generatedAt: getNowIso(),
    captured: false,
    status: "blocked",
    reason: "No ready Android emulator/device was available for logcat capture.",
    output: null,
  };
} else {
  await runAdb(["-s", readiness.device.serial, "logcat", "-d", "-t", "500"], { timeoutMs: 20000 }).then(async (result) => {
    await writeText(logPath, result.stdout || result.stderr || "");
    payload = {
      generatedAt: getNowIso(),
      captured: result.code === 0,
      status: result.code === 0 ? "captured" : "failed",
      serial: readiness.device.serial,
      output: path.relative(process.cwd(), logPath),
      stderr: result.stderr,
    };
  });
}

await writeJson(path.join(EVIDENCE_DIR, "android-logcat-summary.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.captured ? 0 : 1);
