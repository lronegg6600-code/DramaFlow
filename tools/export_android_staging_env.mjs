import path from "node:path";
import { EVIDENCE_DIR, buildExternalUrlStatuses, exportAndroidEnvFromExternalUrls, getNowIso, writeJson } from "./external_staging_url_common.mjs";

const state = await buildExternalUrlStatuses();
const exported = await exportAndroidEnvFromExternalUrls(state.items);
const payload = {
  generatedAt: getNowIso(),
  exported: exported.exported,
  envFile: exported.envFile,
  verifiedCount: state.items.filter((item) => item.status === "verified").length,
  missingKeys: state.items.filter((item) => item.status !== "verified").map((item) => item.key),
  reason: exported.exported ? "Android staging env exported from verified external URLs." : "Skipped due to incomplete external staging URLs.",
};
await writeJson(path.join(EVIDENCE_DIR, "staging-env-export.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(exported.exported ? 0 : 1);
