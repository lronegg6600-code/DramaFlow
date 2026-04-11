import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, rerunWithVerifiedExternalUrls, writeJson } from "./real_external_url_common.mjs";

const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-external-url-second-validation.json"))) ?? { items: [] };
const rerun = await rerunWithVerifiedExternalUrls(validation.items);
const payload = {
  generatedAt: getNowIso(),
  rerunExecuted: rerun.rerunExecuted,
  scripts: rerun.scripts,
  results: rerun.results,
  reason: rerun.reason,
};
await writeJson(path.join(EVIDENCE_DIR, "mobile-rerun-with-verified-external-urls.json"), payload);

const artifactMap = [
  ["mobile_backend_contract_smoke.mjs", "mobile-contract-smoke-after-real-external-urls.json"],
  ["mobile_auth_feed_detail_flow.mjs", "mobile-auth-feed-detail-after-real-external-urls.json"],
  ["mobile_playback_session_flow.mjs", "mobile-playback-after-real-external-urls.json"],
  ["mobile_billing_entitlement_flow.mjs", "mobile-billing-entitlement-after-real-external-urls.json"],
  ["mobile_revoke_restore_flow.mjs", "mobile-revoke-restore-after-real-external-urls.json"],
];

for (const [scriptName, fileName] of artifactMap) {
  const result = rerun.results.find((entry) => entry.script?.endsWith?.(scriptName));
  const itemPayload = {
    generatedAt: getNowIso(),
    script: scriptName,
    status: rerun.rerunExecuted ? (result?.code === 0 ? "pass" : "fail") : "blocked",
    reason: rerun.rerunExecuted
      ? (result?.stderr?.trim() || result?.stdout?.trim() || "Executed.")
      : rerun.reason,
  };
  await writeJson(path.join(EVIDENCE_DIR, fileName), itemPayload);
}

console.log(JSON.stringify(payload, null, 2));
process.exit(rerun.rerunExecuted ? 0 : 1);
