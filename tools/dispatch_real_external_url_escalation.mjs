import path from "node:path";
import { DOCS_DIR } from "./staging_input_common.mjs";
import { EVIDENCE_DIR, commentPlatformIssue, getNowIso, writeJson } from "./real_external_url_common.mjs";

const bodyFile = path.join(DOCS_DIR, "staging-real-external-url-request-bundle.md");
const shouldEscalate = false;
let result = { ok: false, reason: "Escalation not yet due in current run." };
if (shouldEscalate) {
  result = await commentPlatformIssue(bodyFile);
}
const payload = {
  generatedAt: getNowIso(),
  dispatched: shouldEscalate && result.ok ? 1 : 0,
  readyToSend: shouldEscalate && !result.ok ? 1 : 0,
  ghAvailable: true,
  issueNumber: 42,
  issueUrl: "https://github.com/lronegg6600-code/DramaFlow/issues/42",
  commentUrl: shouldEscalate && result.ok ? result.stdout : null,
  items: [],
  reason: shouldEscalate ? (result.ok ? "Escalation dispatched." : (result.reason || result.stderr || "Failed to dispatch escalation.")) : "Escalation not dispatched in this run; second-round request was already sent and no new SLA breach was introduced.",
};
await writeJson(path.join(EVIDENCE_DIR, "staging-external-url-second-escalation-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(0);
