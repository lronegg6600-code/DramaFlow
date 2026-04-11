import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./real_external_url_common.mjs";

const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-external-url-second-validation.json"))) ?? { items: [] };
const accepted = validation.items.filter((item) => item.status === "verified");
const payload = {
  generatedAt: getNowIso(),
  acceptedCount: accepted.length,
  items: accepted,
  commentDispatchCount: 0,
  reason: accepted.length === 0 ? "No verified real external staging URLs available for acceptance." : "Verified real external URLs accepted locally.",
};
await writeJson(path.join(EVIDENCE_DIR, "staging-external-url-second-acceptance-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(accepted.length === 0 ? 1 : 0);
