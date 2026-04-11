import path from "node:path";
import {
  APK_PATH,
  APP_ACTIVITY,
  APP_PACKAGE,
  EVIDENCE_DIR,
  LOCAL_DEBUG_URLS,
  fileExists,
  getNowIso,
  readJsonIfExists,
  writeJson,
} from "./android_local_common.mjs";

const apkExists = await fileExists(APK_PATH);
const localEnvExport = await readJsonIfExists(path.join(EVIDENCE_DIR, "android-local-debug-env-export.json"));

const payload = {
  generatedAt: getNowIso(),
  applicationId: APP_PACKAGE,
  mainActivity: APP_ACTIVITY,
  apkExists,
  apkPath: path.relative(process.cwd(), APK_PATH),
  localDebugEnvExported: localEnvExport?.exported ?? false,
  urls: LOCAL_DEBUG_URLS,
  scope: "local_debug_only",
  ready: apkExists && (localEnvExport?.exported ?? false),
  summary: apkExists && (localEnvExport?.exported ?? false)
    ? "Debug target and local base URLs are ready for emulator/device install."
    : "Debug target is not fully ready for App smoke.",
};

await writeJson(path.join(EVIDENCE_DIR, "android-debug-target-check.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.ready ? 0 : 1);
