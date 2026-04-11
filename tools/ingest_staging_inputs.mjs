import path from "node:path";
import { EVIDENCE_DIR, STAGING_INPUT_ITEMS, buildStagingInputStatuses, getNowIso, writeJson } from "./staging_input_common.mjs";

const state = await buildStagingInputStatuses();
const payload = {
  generatedAt: getNowIso(),
  receivedFile: state.sourceFile,
  manifestFiles: state.manifestFiles,
  urlFiles: state.urlFiles,
  receivedCount: state.items.filter((item) => item.status !== "not_received").length,
  items: state.items.map((item) => ({
    id: item.id,
    key: item.key,
    title: item.title,
    status: item.status,
    sourceFile: item.sourceFile,
    providedValue: item.providedValue,
    envVar: item.envVar,
    owner: item.owner,
  })),
  expectedCount: STAGING_INPUT_ITEMS.length,
};

await writeJson(path.join(EVIDENCE_DIR, "staging-input-intake.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.receivedCount === 0 ? 1 : 0);
