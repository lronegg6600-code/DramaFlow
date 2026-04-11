import path from "node:path";
import { EVIDENCE_DIR, exportVerifiedEnv, getNowIso, readJsonIfExists, writeJson } from "./real_external_url_common.mjs";

const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-external-url-second-validation.json"))) ?? { items: [] };
const result = await exportVerifiedEnv(validation.items);
const payload = {
  generatedAt: getNowIso(),
  exported: result.exported,
  envFile: result.envFile,
  verifiedCount: validation.items.filter((item) => item.status === "verified").length,
  missingKeys: validation.items.filter((item) => item.status === "real_url_not_received").map((item) => item.key),
  invalidKeys: validation.items.filter((item) => item.status === "real_url_received_but_invalid").map((item) => item.key),
  reason: result.exported ? "Verified Android staging env exported." : "Skipped due to unresolved external staging URLs.",
};
await writeJson(path.join(EVIDENCE_DIR, "android-staging-env-verified-export.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(result.exported ? 0 : 1);
