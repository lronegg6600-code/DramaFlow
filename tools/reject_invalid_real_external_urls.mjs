import path from "node:path";
import { EVIDENCE_DIR, buildRealExternalStatuses, getNowIso, writeJson } from "./real_external_url_common.mjs";

const state = await buildRealExternalStatuses();
const rejected = state.items
  .filter((item) => item.status === "real_url_received_but_invalid")
  .map((item) => ({
    id: item.id,
    key: item.key,
    providedValue: item.providedValue,
    status: item.status,
    rejectionReason: item.rejectionReason,
    sourceFile: item.sourceFile,
  }));
const payload = {
  generatedAt: getNowIso(),
  rejectionCount: rejected.length,
  items: rejected,
};
await writeJson(path.join(EVIDENCE_DIR, "staging-real-external-url-rejection-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
