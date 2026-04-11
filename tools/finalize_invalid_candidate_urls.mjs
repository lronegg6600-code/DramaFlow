import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./real_external_url_common.mjs";

const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-base-urls-external-validation.json"))) ?? { items: [] };
const invalidItems = validation.items.filter((item) => item.status === "received_but_invalid");
const payload = {
  generatedAt: getNowIso(),
  closeoutCompleted: invalidItems.length > 0,
  candidateManifest: "platform-intake/received/staging-external-urls/manifests/staging-external-urls.candidate.yaml",
  probeEnvFile: ".env.staging.mobile.candidate",
  invalidCount: invalidItems.length,
  invalidReasonSummary: "Hostname did not resolve",
  lifecycleState: "candidate_received_but_invalid",
  items: invalidItems.map((item) => ({
    id: item.id,
    key: item.key,
    providedValue: item.providedValue,
    status: "candidate_received_but_invalid",
    rejectionReason: item.rejectionReason,
  })),
};
await writeJson(path.join(EVIDENCE_DIR, "staging-candidate-invalid-closeout.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.closeoutCompleted ? 0 : 1);
