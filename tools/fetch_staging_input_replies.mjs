import path from "node:path";
import { EVIDENCE_DIR, getNowIso, writeJson } from "./staging_input_common.mjs";

const payload = {
  generatedAt: getNowIso(),
  repliesProcessed: 0,
  ghAvailable: false,
  items: [],
  reason: "GitHub CLI is unavailable in the current environment; no live staging input replies were fetched.",
};
await writeJson(path.join(EVIDENCE_DIR, "staging-input-reply-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(1);
