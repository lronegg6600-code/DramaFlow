import path from "node:path";
import { DOCS_DIR } from "./staging_input_common.mjs";
import { EVIDENCE_DIR, commentPlatformIssue, getNowIso, writeJson } from "./real_external_url_common.mjs";

const bodyFile = path.join(DOCS_DIR, "staging-real-external-url-request-bundle.md");
const result = await commentPlatformIssue(bodyFile);
const payload = {
  generatedAt: getNowIso(),
  dispatched: result.ok,
  issueNumber: 42,
  issueUrl: "https://github.com/lronegg6600-code/DramaFlow/issues/42",
  bodyFile: "docs/staging-real-external-url-request-bundle.md",
  commentUrl: result.ok ? result.stdout : null,
  stderr: result.stderr,
  reason: result.ok
    ? "Second-round real external URL request dispatched to platform issue."
    : (result.reason || result.stderr || "Failed to dispatch second-round request."),
};
await writeJson(path.join(EVIDENCE_DIR, "staging-external-url-second-reminder-log.json"), {
  generatedAt: getNowIso(),
  reminderType: "second_round_request",
  dispatched: result.ok ? 1 : 0,
  readyToSend: result.ok ? 0 : 1,
  ghAvailable: true,
  issueNumber: 42,
  issueUrl: "https://github.com/lronegg6600-code/DramaFlow/issues/42",
  commentUrl: result.ok ? result.stdout : null,
  items: [],
  reason: payload.reason,
});
console.log(JSON.stringify(payload, null, 2));
process.exit(result.ok ? 0 : 1);
