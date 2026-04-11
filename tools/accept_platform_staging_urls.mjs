import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./external_staging_url_common.mjs";

const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-base-urls-external-validation.json"))) ?? { items: [] };
const accepted = validation.items.filter((item) => item.status === "verified");
const payload = {
  generatedAt: getNowIso(),
  acceptedCount: accepted.length,
  items: accepted,
  commentDispatchCount: 0,
  reason: accepted.length === 0 ? "No verified external staging URLs available for acceptance." : "Verified external staging URLs accepted locally; live issue commenting not executed in current environment.",
};
await writeJson(path.join(EVIDENCE_DIR, "staging-external-url-acceptance-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(accepted.length === 0 ? 1 : 0);
