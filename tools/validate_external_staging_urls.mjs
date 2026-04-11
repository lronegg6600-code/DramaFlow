import path from "node:path";
import { EVIDENCE_DIR, EXTERNAL_URL_ITEMS, buildExternalUrlStatuses, getNowIso, summarizeExternalStatuses, writeJson } from "./external_staging_url_common.mjs";

const state = await buildExternalUrlStatuses();
const statuses = summarizeExternalStatuses(state.items);
const payload = {
  generatedAt: getNowIso(),
  expectedCount: EXTERNAL_URL_ITEMS.length,
  statuses,
  allVerified: statuses.verified === EXTERNAL_URL_ITEMS.length,
  items: state.items.map((item) => ({
    id: item.id,
    key: item.key,
    title: item.title,
    status: item.status,
    providedValue: item.providedValue,
    rejectionReason: item.rejectionReason,
    owner: item.owner,
    envVar: item.envVar,
    external_required: true,
    internal_only_acceptable: false,
  })),
};
await writeJson(path.join(EVIDENCE_DIR, "staging-base-urls-external-validation.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.allVerified ? 0 : 1);
