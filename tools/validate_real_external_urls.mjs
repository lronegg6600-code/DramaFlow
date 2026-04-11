import path from "node:path";
import { EVIDENCE_DIR, REAL_EXTERNAL_URL_ITEMS, buildRealExternalStatuses, getNowIso, summarizeRealStatuses, writeJson } from "./real_external_url_common.mjs";

const state = await buildRealExternalStatuses();
const statuses = summarizeRealStatuses(state.items);
const payload = {
  generatedAt: getNowIso(),
  expectedCount: REAL_EXTERNAL_URL_ITEMS.length,
  statuses,
  allVerified: statuses.verified === REAL_EXTERNAL_URL_ITEMS.length,
  items: state.items.map((item) => ({
    id: item.id,
    key: item.key,
    title: item.title,
    status: item.status,
    providedValue: item.providedValue,
    rejectionReason: item.rejectionReason,
    owner: item.owner,
    envVar: item.envVar,
  })),
};
await writeJson(path.join(EVIDENCE_DIR, "staging-external-url-second-validation.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.allVerified ? 0 : 1);
