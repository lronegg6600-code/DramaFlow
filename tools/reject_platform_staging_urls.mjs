import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./external_staging_url_common.mjs";

const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-base-urls-external-validation.json"))) ?? { items: [] };
const rejected = validation.items.filter((item) => item.status === "received_but_invalid");
const payload = {
  generatedAt: getNowIso(),
  rejectedCount: rejected.length,
  items: rejected,
  commentDispatchCount: 0,
  reason: rejected.length === 0 ? "No invalid external staging URL found." : "Invalid external staging URLs rejected locally; live issue commenting not executed in current environment.",
};
await writeJson(path.join(EVIDENCE_DIR, "staging-external-url-rejection-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(rejected.length === 0 ? 1 : 0);
