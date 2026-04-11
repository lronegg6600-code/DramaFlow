import path from "node:path";
import { EVIDENCE_DIR, EXTERNAL_URL_ITEMS, buildExternalUrlStatuses, getNowIso, readJsonIfExists, summarizeExternalStatuses, writeJson } from "./external_staging_url_common.mjs";

const state = await buildExternalUrlStatuses();
const statuses = summarizeExternalStatuses(state.items);
const rerun = (await readJsonIfExists(path.join(EVIDENCE_DIR, "mobile-rerun-after-external-urls.json"))) ?? { rerunExecuted: false };
const payload = {
  generatedAt: getNowIso(),
  internalServiceMapConfirmed: true,
  externalUrlReplyCount: 0,
  externalUrlReceivedCount: state.items.filter((item) => item.status !== "not_received").length,
  statuses,
  allVerified: statuses.verified === EXTERNAL_URL_ITEMS.length,
  androidEnvExported: statuses.verified === EXTERNAL_URL_ITEMS.length,
  integrationRerunExecuted: rerun.rerunExecuted,
  currentState: rerun.rerunExecuted ? "integration_rerun_executed" : statuses.verified === EXTERNAL_URL_ITEMS.length ? "partially_unblocked" : "still_blocked_by_missing_external_staging_urls",
  items: state.items.map((item) => ({
    id: item.id,
    key: item.key,
    status: item.status,
    providedValue: item.providedValue,
    rejectionReason: item.rejectionReason,
  })),
};
await writeJson(path.join(EVIDENCE_DIR, "staging-external-url-burndown.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.allVerified ? 0 : 1);
