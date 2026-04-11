import path from "node:path";
import { EVIDENCE_DIR, STAGING_INPUT_ITEMS, getNowIso, readJsonIfExists, summarizeItemStatuses, writeJson } from "./staging_input_common.mjs";

const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-input-validation.json"))) ?? {
  items: STAGING_INPUT_ITEMS.map((item) => ({ ...item, status: "not_received", providedValue: null })),
};
const rerun = (await readJsonIfExists(path.join(EVIDENCE_DIR, "mobile-rerun-with-staging-inputs.json"))) ?? { rerunExecuted: false };
const summary = summarizeItemStatuses(validation.items);
const allVerified = summary.verified === STAGING_INPUT_ITEMS.length;
const payload = {
  generatedAt: getNowIso(),
  replyCount: 0,
  receivedCount: validation.items.filter((item) => item.status !== "not_received").length,
  statuses: summary,
  allVerified,
  envExported: allVerified,
  integrationRerunExecuted: rerun.rerunExecuted,
  currentState: rerun.rerunExecuted ? "integration_rerun_executed" : allVerified ? "partially_unblocked" : "still_blocked_by_missing_staging_inputs",
  items: validation.items.map((item) => ({
    id: item.id,
    key: item.key,
    title: item.title,
    status: item.status,
    providedValue: item.providedValue ?? null,
    rejectionReason: item.rejectionReason ?? null,
  })),
};
await writeJson(path.join(EVIDENCE_DIR, "staging-input-burndown.json"), payload);
await writeJson(path.join(EVIDENCE_DIR, "mobile-integration-resume-summary.json"), {
  generatedAt: payload.generatedAt,
  currentState: payload.currentState,
  rerunExecuted: rerun.rerunExecuted,
  allVerified,
  readyForFullMobileRerun: allVerified,
  remaining: validation.items.filter((item) => item.status !== "verified").map((item) => ({ key: item.key, status: item.status })),
});
console.log(JSON.stringify(payload, null, 2));
process.exit(allVerified ? 0 : 1);
