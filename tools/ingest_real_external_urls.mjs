import path from "node:path";
import { EVIDENCE_DIR, REAL_EXTERNAL_URL_ITEMS, buildRealExternalStatuses, getNowIso, writeJson } from "./real_external_url_common.mjs";

const state = await buildRealExternalStatuses();
const payload = {
  generatedAt: getNowIso(),
  receivedFile: state.sourceFile,
  responseFiles: state.responseFiles,
  manifestFiles: state.manifestFiles,
  urlFiles: state.urlFiles,
  receivedCount: state.items.filter((item) => item.status !== "real_url_not_received").length,
  expectedCount: REAL_EXTERNAL_URL_ITEMS.length,
  items: state.items.map((item) => ({
    id: item.id,
    key: item.key,
    status: item.status,
    sourceFile: item.sourceFile,
    providedValue: item.providedValue,
    owner: item.owner,
    envVar: item.envVar,
  })),
};
await writeJson(path.join(EVIDENCE_DIR, "staging-external-url-second-intake.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.receivedCount === 0 ? 1 : 0);
