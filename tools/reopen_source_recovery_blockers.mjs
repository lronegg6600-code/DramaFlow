import path from "node:path";
import { EVIDENCE_DIR, getNowIso, writeJson } from "./source_recovery_common.mjs";

const payload = {
  generatedAt: getNowIso(),
  reopenedCount: 0,
  items: [],
  reason: "No previously closed source recovery blocker required reopen in this cycle.",
};
await writeJson(path.join(EVIDENCE_DIR, "source-recovery-reopen-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(0);
