import path from "node:path";
import {
  EVIDENCE_DIR,
  getNowIso,
  readJsonIfExists,
  writeJson,
} from "./real_external_url_common.mjs";

const replyLog = await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-real-external-url-reply-log.json"));
const intake = await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-external-url-second-intake.json"));
const validation = await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-external-url-second-validation.json"));
const summary = {
  generatedAt: getNowIso(),
  platformReplyCount: replyLog?.platformReplyCount ?? 0,
  realExternalUrlReceivedCount: intake?.receivedCount ?? 0,
  statuses: validation?.statuses ?? {
    real_url_not_received: 7,
    real_url_received_but_invalid: 0,
    verified: 0,
    closed: 0,
    escalated: 0,
  },
  currentState:
    (validation?.statuses?.verified ?? 0) === 7
      ? "verified"
      : (validation?.statuses?.real_url_received_but_invalid ?? 0) > 0
        ? "real_url_received_but_invalid"
        : "real_url_not_received",
};
await writeJson(path.join(EVIDENCE_DIR, "staging-real-external-url-status-summary.json"), summary);
console.log(JSON.stringify(summary, null, 2));
