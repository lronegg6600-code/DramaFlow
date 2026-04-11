import path from "node:path";
import { EVIDENCE_DIR, INTERNAL_SERVICE_MAP, getNowIso, writeJson } from "./external_staging_url_common.mjs";

const payload = {
  generatedAt: getNowIso(),
  confirmed: true,
  layer: "internal_cluster_service_map",
  items: INTERNAL_SERVICE_MAP,
};

await writeJson(path.join(EVIDENCE_DIR, "staging-internal-service-map.json"), payload);
await writeJson(path.join(EVIDENCE_DIR, "staging-url-layer-separation.json"), {
  generatedAt: payload.generatedAt,
  internalServiceMapConfirmed: true,
  externalAndroidAccessibleUrlsReceived: false,
  blocker: "missing_external_staging_gateway_or_domain_mapping",
  internal_only_keys: INTERNAL_SERVICE_MAP.map((item) => item.key),
  note: "Internal cluster URLs are confirmed from repo files, but Android external access is not verified.",
});
console.log(JSON.stringify(payload, null, 2));
