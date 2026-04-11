import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./staging_input_common.mjs";

const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-input-validation.json"))) ?? { items: [] };
const accepted = validation.items.filter((item) => item.status === "verified");
const payload = {
  generatedAt: getNowIso(),
  acceptedCount: accepted.length,
  items: accepted,
  commentDispatchCount: 0,
  reason: accepted.length === 0 ? "No verified staging inputs available for acceptance." : "Verified staging inputs accepted locally; live issue commenting not executed in current environment.",
};
await writeJson(path.join(EVIDENCE_DIR, "staging-input-acceptance-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(accepted.length === 0 ? 1 : 0);
