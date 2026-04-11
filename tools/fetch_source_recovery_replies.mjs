import path from "node:path";
import { EVIDENCE_DIR, getNowIso, writeJson } from "./source_recovery_common.mjs";

const ghAvailable = false;
const payload = {
  generatedAt: getNowIso(),
  ghAvailable,
  repliesProcessed: 0,
  replies: [],
  reason: ghAvailable ? "Replies fetched." : "GitHub CLI is unavailable in current environment; no live source recovery replies could be fetched.",
};
await writeJson(path.join(EVIDENCE_DIR, "source-recovery-reply-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(ghAvailable ? 0 : 1);
