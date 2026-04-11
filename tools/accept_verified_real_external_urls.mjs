import path from "node:path";
import { EVIDENCE_DIR, buildRealExternalStatuses, getNowIso, writeJson } from "./real_external_url_common.mjs";

const state = await buildRealExternalStatuses();
const accepted = state.items.filter((item) => item.status === "verified").map((item) => ({
  id: item.id,
  key: item.key,
  providedValue: item.providedValue,
  status: item.status,
  sourceFile: item.sourceFile,
}));
const payload = {
  generatedAt: getNowIso(),
  acceptanceCount: accepted.length,
  items: accepted,
};
await writeJson(path.join(EVIDENCE_DIR, "staging-real-external-url-acceptance-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
