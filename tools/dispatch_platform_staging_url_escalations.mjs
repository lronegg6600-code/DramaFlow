import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./external_staging_url_common.mjs";

const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-base-urls-external-validation.json"))) ?? { items: [] };
const queue = validation.items.filter((item) => item.status === "received_but_invalid" || item.status === "escalated");
const payload = {
  generatedAt: getNowIso(),
  escalationCount: queue.length,
  dispatched: 0,
  readyToSend: queue.length,
  ghAvailable: false,
  items: queue,
  reason: queue.length === 0 ? "No platform staging URL escalation required." : "Escalation queue prepared, but no live GitHub dispatch was performed in current environment.",
};
await writeJson(path.join(EVIDENCE_DIR, "staging-external-url-escalation-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(queue.length === 0 ? 0 : 1);
