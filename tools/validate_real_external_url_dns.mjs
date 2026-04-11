import path from "node:path";
import {
  EVIDENCE_DIR,
  REAL_EXTERNAL_URL_ITEMS,
  buildRealExternalStatuses,
  checkDnsResolution,
  getNowIso,
  writeJson,
} from "./real_external_url_common.mjs";

const state = await buildRealExternalStatuses();
const items = [];
for (const item of REAL_EXTERNAL_URL_ITEMS) {
  const current = state.items.find((entry) => entry.key === item.key);
  const result = current?.providedValue ? await checkDnsResolution(current.providedValue) : null;
  items.push({
    id: item.id,
    key: item.key,
    status: current?.status ?? "real_url_not_received",
    providedValue: current?.providedValue ?? null,
    dnsOk: result?.ok ?? false,
    reason: result ? (result.ok ? `Resolved ${result.hostname}` : result.reason) : "Not received",
  });
}
const payload = {
  generatedAt: getNowIso(),
  verifiedCount: items.filter((item) => item.dnsOk).length,
  items,
};
await writeJson(path.join(EVIDENCE_DIR, "staging-real-external-url-dns-check.json"), payload);
console.log(JSON.stringify(payload, null, 2));
