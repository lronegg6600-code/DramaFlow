import path from "node:path";
import { EVIDENCE_DIR, INTERNAL_SERVICE_MAP, ROOT, getNowIso, writeJson, writeText } from "./external_staging_url_common.mjs";

const yaml = INTERNAL_SERVICE_MAP.map((item) => `${item.key}: ${item.url}`).join("\n");
const draftFile = path.join(ROOT, "platform-intake", "received", "staging-inputs", "manifests", "staging-base-urls.internal-draft.yaml");
await writeText(draftFile, `${yaml}\n`);

const payload = {
  generatedAt: getNowIso(),
  draftFile: path.relative(ROOT, draftFile),
  layer: "internal_only",
  items: INTERNAL_SERVICE_MAP,
  usagePolicy: {
    allowed: ["cluster_internal_service_calls", "port_forward_reference", "manifest_placeholder"],
    forbidden: ["android_direct_external_access", "public_gateway_claim", "staging_external_url_acceptance"],
  },
};

await writeJson(path.join(EVIDENCE_DIR, "staging-base-urls-internal-draft.json"), payload);
console.log(JSON.stringify(payload, null, 2));
