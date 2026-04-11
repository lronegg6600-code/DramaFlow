import path from "node:path";
import { EVIDENCE_DIR, EXTERNAL_URL_ITEMS, buildExternalUrlStatuses, getNowIso, writeJson } from "./external_staging_url_common.mjs";

const state = await buildExternalUrlStatuses();
const payload = {
  generatedAt: getNowIso(),
  receivedFile: state.sourceFile,
  manifestFiles: state.manifestFiles,
  urlFiles: state.urlFiles,
  receivedCount: state.items.filter((item) => item.status !== "not_received").length,
  expectedCount: EXTERNAL_URL_ITEMS.length,
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
await writeJson(path.join(EVIDENCE_DIR, "staging-base-urls-external-intake.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.receivedCount === 0 ? 1 : 0);
