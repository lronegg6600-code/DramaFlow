import path from "node:path";
import { EVIDENCE_DIR, LOCAL_DEBUG_URLS, getNowIso, writeJson, writeLocalDebugEnvFile } from "./local_docker_common.mjs";

const envFile = await writeLocalDebugEnvFile();
const payload = {
  generatedAt: getNowIso(),
  exported: true,
  scope: "local_debug_only",
  envFile,
  urls: LOCAL_DEBUG_URLS,
  reason: "Android debug build already targets 10.0.2.2; local debug env exported for explicit validation.",
};
await writeJson(path.join(EVIDENCE_DIR, "android-local-debug-env-export.json"), payload);
console.log(JSON.stringify(payload, null, 2));
