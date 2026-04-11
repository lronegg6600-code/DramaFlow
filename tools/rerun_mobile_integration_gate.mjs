import path from "node:path";
import { EVIDENCE_DIR, ROOT, getNowIso, readJsonIfExists, runNode, writeJson } from "./source_recovery_common.mjs";

const repo = await readJsonIfExists(path.join(EVIDENCE_DIR, "rehydrated-repo-identity.json"));
const android = await readJsonIfExists(path.join(EVIDENCE_DIR, "rehydrated-android-tree.json"));
const backend = await readJsonIfExists(path.join(EVIDENCE_DIR, "rehydrated-backend-tree.json"));
const restored = repo?.status === "restored" && android?.status === "restored" && backend?.status === "restored";
const scripts = [
  "mobile_backend_contract_smoke.mjs",
  "mobile_auth_feed_detail_flow.mjs",
  "mobile_playback_session_flow.mjs",
  "mobile_billing_entitlement_flow.mjs",
  "mobile_revoke_restore_flow.mjs",
];
const results = [];
if (restored) {
  for (const script of scripts) {
    results.push(await runNode(path.join(ROOT, "backend", "tests", "integration", script)));
  }
}
const payload = { generatedAt: getNowIso(), rerunExecuted: restored, reason: restored ? "Source trees restored; gate rerun executed." : "Skipped because source recovery incomplete.", scripts, results };
await writeJson(path.join(EVIDENCE_DIR, "integration-rerun-after-recovery.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(restored ? 0 : 1);
