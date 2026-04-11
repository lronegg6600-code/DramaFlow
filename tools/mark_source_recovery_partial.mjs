import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./source_recovery_common.mjs";

const validation = await readJsonIfExists(path.join(EVIDENCE_DIR, "source-recovery-validation.json"));
const partial = (validation?.items ?? []).filter((item) => item.status === "received_partial");
const payload = {
  generatedAt: getNowIso(),
  partialCount: partial.length,
  items: partial,
  commentDispatchCount: 0,
  reason: partial.length === 0 ? "No partial source recovery input detected." : "Partial source recovery input logged; live issue partial-acceptance comment not dispatched in current environment.",
};
await writeJson(path.join(EVIDENCE_DIR, "source-recovery-partial-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(partial.length === 0 ? 0 : 1);
