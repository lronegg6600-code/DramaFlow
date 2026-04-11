import path from "node:path";
import { EVIDENCE_DIR, STAGING_INPUT_ITEMS, buildStagingInputStatuses, getNowIso, summarizeItemStatuses, writeJson } from "./staging_input_common.mjs";

const state = await buildStagingInputStatuses();
const summary = summarizeItemStatuses(state.items);
const payload = {
  generatedAt: getNowIso(),
  expectedCount: STAGING_INPUT_ITEMS.length,
  statuses: summary,
  allVerified: summary.verified === STAGING_INPUT_ITEMS.length,
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

await writeJson(path.join(EVIDENCE_DIR, "staging-input-validation.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.allVerified ? 0 : 1);
