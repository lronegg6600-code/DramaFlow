import path from "node:path";
import { EVIDENCE_DIR, ROOT, STAGING_INPUT_ITEMS, getNowIso, readJsonIfExists, runNodeScript, writeJson } from "./staging_input_common.mjs";

const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-input-validation.json"))) ?? { items: [] };
const verified = new Map(validation.items.filter((item) => item.status === "verified").map((item) => [item.key, item.providedValue]));
const allVerified = STAGING_INPUT_ITEMS.every((item) => verified.has(item.key));
const env = Object.fromEntries(STAGING_INPUT_ITEMS.map((item) => [item.envVar, verified.get(item.key) ?? ""]));
const scripts = [
  "mobile_backend_contract_smoke.mjs",
  "mobile_auth_feed_detail_flow.mjs",
  "mobile_playback_session_flow.mjs",
  "mobile_billing_entitlement_flow.mjs",
  "mobile_revoke_restore_flow.mjs",
];
const results = [];
if (allVerified) {
  for (const script of scripts) {
    results.push(await runNodeScript(path.join(ROOT, "backend", "tests", "integration", script), env));
  }
}
const payload = {
  generatedAt: getNowIso(),
  rerunExecuted: allVerified,
  scripts,
  results,
  reason: allVerified ? "Staging inputs verified; mobile integration rerun executed." : "Skipped due to incomplete staging inputs.",
};
await writeJson(path.join(EVIDENCE_DIR, "mobile-rerun-with-staging-inputs.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(allVerified ? 0 : 1);
