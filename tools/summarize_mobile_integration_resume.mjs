import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./external_staging_url_common.mjs";

const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-base-urls-external-validation.json"))) ?? { statuses: { not_received: 7, received_but_invalid: 0, verified: 0, closed: 0, escalated: 0 }, allVerified: false };
const rerun = (await readJsonIfExists(path.join(EVIDENCE_DIR, "mobile-rerun-after-external-urls.json"))) ?? { rerunExecuted: false, results: [], reason: "Skipped due to incomplete external staging URLs." };
const payload = {
  generatedAt: getNowIso(),
  currentState: rerun.rerunExecuted ? "integration_rerun_executed" : validation.allVerified ? "partially_unblocked" : "still_blocked_by_missing_external_staging_urls",
  verifiedExternalUrls: validation.statuses.verified ?? 0,
  invalidExternalUrls: validation.statuses.received_but_invalid ?? 0,
  missingExternalUrls: validation.statuses.not_received ?? 0,
  rerunExecuted: rerun.rerunExecuted,
  reason: rerun.rerunExecuted ? "External staging URL rerun executed." : rerun.reason,
};
await writeJson(path.join(EVIDENCE_DIR, "mobile-integration-resume-summary.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(rerun.rerunExecuted ? 0 : 1);
