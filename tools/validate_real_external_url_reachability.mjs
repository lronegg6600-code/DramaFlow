import path from "node:path";
import {
  EVIDENCE_DIR,
  REAL_EXTERNAL_URL_ITEMS,
  buildRealExternalStatuses,
  checkBasicReachability,
  getNowIso,
  writeJson,
} from "./real_external_url_common.mjs";

const state = await buildRealExternalStatuses();
const items = [];
for (const item of REAL_EXTERNAL_URL_ITEMS) {
  const current = state.items.find((entry) => entry.key === item.key);
  const result = current?.providedValue && current.status === "verified"
    ? await checkBasicReachability(current.providedValue)
    : null;
  items.push({
    id: item.id,
    key: item.key,
    status: current?.status ?? "real_url_not_received",
    providedValue: current?.providedValue ?? null,
    reachable: result?.ok ?? false,
    reason: result ? result.reason : (current?.providedValue ? "Skipped because URL is not fully verified." : "Not received"),
    httpStatus: result?.status ?? null,
  });
}
const payload = {
  generatedAt: getNowIso(),
  reachableCount: items.filter((item) => item.reachable).length,
  items,
};
await writeJson(path.join(EVIDENCE_DIR, "staging-real-external-url-reachability-check.json"), payload);
console.log(JSON.stringify(payload, null, 2));
