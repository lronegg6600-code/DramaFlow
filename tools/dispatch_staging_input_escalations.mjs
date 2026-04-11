import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./staging_input_common.mjs";

const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-input-validation.json"))) ?? { items: [] };
const queue = validation.items.filter((item) => item.status === "received_but_invalid");
const payload = {
  generatedAt: getNowIso(),
  escalationCount: queue.length,
  dispatched: 0,
  readyToSend: queue.length,
  ghAvailable: false,
  items: queue,
  reason: queue.length === 0 ? "No staging input escalation required." : "Escalation queue prepared, but no live GitHub dispatch was performed in current environment.",
};
await writeJson(path.join(EVIDENCE_DIR, "staging-input-escalation-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(queue.length === 0 ? 0 : 1);
