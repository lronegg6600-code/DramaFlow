import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./external_staging_url_common.mjs";

const source = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-env-export.json"))) ?? {
  generatedAt: getNowIso(),
  exported: false,
  envFile: null,
  verifiedCount: 0,
  missingKeys: [],
  reason: "Skipped due to incomplete external staging URLs.",
};

await writeJson(path.join(EVIDENCE_DIR, "android-staging-env-export.json"), source);
console.log(JSON.stringify(source, null, 2));
process.exit(source.exported ? 0 : 1);
