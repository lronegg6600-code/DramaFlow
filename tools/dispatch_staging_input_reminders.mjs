import path from "node:path";
import { EVIDENCE_DIR, STAGING_INPUT_ITEMS, getNowIso, readJsonIfExists, writeJson } from "./staging_input_common.mjs";

const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-input-validation.json"))) ?? { items: STAGING_INPUT_ITEMS.map((item) => ({ ...item, status: "not_received" })) };
const queue = validation.items.filter((item) => item.status === "not_received");
const payload = {
  generatedAt: getNowIso(),
  reminderCount: queue.length,
  dispatched: 0,
  readyToSend: queue.length,
  ghAvailable: false,
  items: queue,
  reason: queue.length === 0 ? "No staging input reminder required." : "Reminder queue prepared, but no live GitHub dispatch was performed in current environment.",
};
await writeJson(path.join(EVIDENCE_DIR, "staging-input-reminder-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(queue.length === 0 ? 0 : 1);
