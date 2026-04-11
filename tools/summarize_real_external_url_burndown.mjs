import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./real_external_url_common.mjs";

const closeout = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-candidate-invalid-closeout.json"))) ?? { closeoutCompleted: false, invalidCount: 0 };
const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-external-url-second-validation.json"))) ?? {
  statuses: { candidate_received_but_invalid: 0, real_url_not_received: 7, real_url_received_but_invalid: 0, verified: 0, closed: 0, escalated: 0 },
  items: [],
  allVerified: false,
};
const envExport = (await readJsonIfExists(path.join(EVIDENCE_DIR, "android-staging-env-verified-export.json"))) ?? { exported: false };
const rerun = (await readJsonIfExists(path.join(EVIDENCE_DIR, "mobile-rerun-with-verified-external-urls.json"))) ?? { rerunExecuted: false, reason: "Skipped due to unresolved external staging URLs." };

const payload = {
  generatedAt: getNowIso(),
  candidateInvalidCloseoutCompleted: closeout.closeoutCompleted,
  candidateInvalidCount: closeout.invalidCount ?? 0,
  realExternalUrlReceivedCount: validation.items.filter((item) => item.status !== "real_url_not_received").length,
  statuses: validation.statuses,
  verifiedEnvExported: envExport.exported,
  rerunExecuted: rerun.rerunExecuted,
  currentState: rerun.rerunExecuted ? "integration_rerun_executed" : validation.allVerified ? "external_urls_verified_and_mobile_rerun_started" : "still_blocked_by_unresolved_external_staging_urls",
  items: validation.items,
  reason: rerun.rerunExecuted ? "Verified real external staging URLs triggered mobile rerun." : "Candidate invalid closeout is complete, but no verified real external staging URLs have been provided.",
};

await writeJson(path.join(EVIDENCE_DIR, "staging-external-url-second-burndown.json"), payload);
await writeJson(path.join(EVIDENCE_DIR, "mobile-integration-resume-summary.json"), {
  generatedAt: getNowIso(),
  currentState: payload.currentState,
  candidateInvalidCloseoutCompleted: payload.candidateInvalidCloseoutCompleted,
  verifiedExternalUrls: validation.statuses.verified ?? 0,
  invalidRealExternalUrls: validation.statuses.real_url_received_but_invalid ?? 0,
  missingRealExternalUrls: validation.statuses.real_url_not_received ?? 0,
  rerunExecuted: rerun.rerunExecuted,
  reason: payload.reason,
});
console.log(JSON.stringify(payload, null, 2));
process.exit(rerun.rerunExecuted ? 0 : 1);
