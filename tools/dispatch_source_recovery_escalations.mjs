import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./source_recovery_common.mjs";

const ownerStatus = (await readJsonIfExists(path.join(EVIDENCE_DIR, "source-recovery-owner-status.json")))?.items ?? [];
const queue = ownerStatus.filter((item) => item.status === "received_but_invalid" || item.status === "escalated");
const payload = {
  generatedAt: getNowIso(),
  escalationCount: queue.length,
  dispatched: 0,
  ghAvailable: false,
  items: queue,
  reason: queue.length === 0 ? "No escalation candidates met the current transition rules." : "Escalation queue prepared, but live dispatch was not performed in current environment.",
};
await writeJson(path.join(EVIDENCE_DIR, "source-recovery-escalation-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(queue.length === 0 ? 0 : 1);
