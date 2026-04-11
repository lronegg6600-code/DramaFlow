import path from "node:path";
import { EVIDENCE_DIR, EXTERNAL_URL_ITEMS, getNowIso, readJsonIfExists, writeJson } from "./external_staging_url_common.mjs";

const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-base-urls-external-validation.json"))) ?? {
  items: EXTERNAL_URL_ITEMS.map((item) => ({ ...item, status: "not_received" })),
};
const queue = validation.items.filter((item) => item.status === "not_received");
const payload = {
  generatedAt: getNowIso(),
  reminderCount: queue.length,
  dispatched: 0,
  readyToSend: queue.length,
  ghAvailable: false,
  items: queue,
  reason: queue.length === 0 ? "No platform staging URL reminder required." : "Reminder queue prepared, but no live GitHub dispatch was performed in current environment.",
};
await writeJson(path.join(EVIDENCE_DIR, "staging-external-url-reminder-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(queue.length === 0 ? 0 : 1);
