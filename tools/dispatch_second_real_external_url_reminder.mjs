import path from "node:path";
import { DOCS_DIR } from "./staging_input_common.mjs";
import { EVIDENCE_DIR, commentPlatformIssue, getNowIso, writeJson } from "./real_external_url_common.mjs";

const bodyFile = path.join(DOCS_DIR, "staging-real-external-url-reminder-report.md");
const result = await commentPlatformIssue(bodyFile);
const payload = {
  generatedAt: getNowIso(),
  dispatched: result.ok ? 1 : 0,
  readyToSend: result.ok ? 0 : 1,
  ghAvailable: true,
  issueNumber: 42,
  issueUrl: "https://github.com/lronegg6600-code/DramaFlow/issues/42",
  commentUrl: result.ok ? result.stdout : null,
  items: [],
  reason: result.ok ? "Second-round reminder for real external URLs dispatched." : (result.reason || result.stderr || "Failed to dispatch second-round reminder."),
};
await writeJson(path.join(EVIDENCE_DIR, "staging-real-external-url-reminder-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(result.ok ? 0 : 1);
