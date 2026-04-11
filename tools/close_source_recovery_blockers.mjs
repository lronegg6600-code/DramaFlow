import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./source_recovery_common.mjs";

const verified = await readJsonIfExists(path.join(EVIDENCE_DIR, "source-recovery-verified-log.json"));
const closable = verified?.items ?? [];
const payload = {
  generatedAt: getNowIso(),
  closedCount: closable.length,
  items: closable,
  reason: closable.length === 0 ? "No source recovery blocker qualified for closeout." : "Verified blockers are close-ready; no live issue close executed in current environment.",
};
await writeJson(path.join(EVIDENCE_DIR, "source-recovery-closeout-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(closable.length === 0 ? 1 : 0);
