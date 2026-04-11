import path from "node:path";
import {
  EVIDENCE_DIR,
  REAL_EXTERNAL_URL_ITEMS,
  buildRealExternalStatuses,
  checkPathPresence,
  getNowIso,
  writeJson,
} from "./real_external_url_common.mjs";

const state = await buildRealExternalStatuses();
const items = REAL_EXTERNAL_URL_ITEMS.map((item) => {
  const current = state.items.find((entry) => entry.key === item.key);
  const result = current?.providedValue ? checkPathPresence(current.providedValue) : null;
  return {
    id: item.id,
    key: item.key,
    status: current?.status ?? "real_url_not_received",
    providedValue: current?.providedValue ?? null,
    pathOk: result?.ok ?? false,
    reason: result ? (result.ok ? `Path ${result.path}` : result.reason) : "Not received",
  };
});
const payload = {
  generatedAt: getNowIso(),
  pathOkCount: items.filter((item) => item.pathOk).length,
  items,
};
await writeJson(path.join(EVIDENCE_DIR, "staging-real-external-url-path-check.json"), payload);
console.log(JSON.stringify(payload, null, 2));
