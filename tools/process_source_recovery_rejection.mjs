import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./source_recovery_common.mjs";

const validation = await readJsonIfExists(path.join(EVIDENCE_DIR, "source-recovery-validation.json"));
const rejected = (validation?.items ?? []).filter((item) => item.status === "received_but_invalid");
const payload = {
  generatedAt: getNowIso(),
  rejectedCount: rejected.length,
  items: rejected,
  commentDispatchCount: 0,
  reason: rejected.length === 0 ? "No invalid source recovery input detected." : "Invalid source recovery input logged; live issue rejection comment not dispatched in current environment.",
};
await writeJson(path.join(EVIDENCE_DIR, "source-recovery-rejection-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(rejected.length === 0 ? 0 : 1);
