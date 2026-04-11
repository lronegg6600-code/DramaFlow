import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./source_recovery_common.mjs";

const ownerStatus = (await readJsonIfExists(path.join(EVIDENCE_DIR, "source-recovery-owner-status.json")))?.items ?? [];
const queue = ownerStatus.filter((item) => ["not_received", "awaiting_reply", "received_partial"].includes(item.status));
const payload = {
  generatedAt: getNowIso(),
  reminderCount: queue.length,
  dispatched: 0,
  readyToSend: queue.length,
  ghAvailable: false,
  items: queue,
  reason: queue.length === 0 ? "No source recovery reminders required." : "Reminder queue prepared, but no live GitHub dispatch was performed in current environment.",
};
await writeJson(path.join(EVIDENCE_DIR, "source-recovery-reminder-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(queue.length === 0 ? 0 : 1);
