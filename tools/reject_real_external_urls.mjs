import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./real_external_url_common.mjs";

const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-external-url-second-validation.json"))) ?? { items: [] };
const rejected = validation.items.filter((item) => item.status === "real_url_received_but_invalid");
const payload = {
  generatedAt: getNowIso(),
  rejectedCount: rejected.length,
  items: rejected,
  commentDispatchCount: 0,
  reason: rejected.length === 0 ? "No invalid real external staging URL found." : "Invalid real external URLs rejected locally.",
};
await writeJson(path.join(EVIDENCE_DIR, "staging-external-url-second-rejection-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(rejected.length === 0 ? 1 : 0);
