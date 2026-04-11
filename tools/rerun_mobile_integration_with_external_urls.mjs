import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./external_staging_url_common.mjs";

const rerun = (await readJsonIfExists(path.join(EVIDENCE_DIR, "mobile-rerun-after-external-urls.json"))) ?? {
  generatedAt: getNowIso(),
  rerunExecuted: false,
  scripts: [],
  results: [],
  reason: "Skipped due to incomplete external staging URLs.",
};

const fileMap = [
  ["mobile_backend_contract_smoke.mjs", "mobile-contract-smoke-after-external-urls.json"],
  ["mobile_auth_feed_detail_flow.mjs", "mobile-auth-feed-detail-after-external-urls.json"],
  ["mobile_playback_session_flow.mjs", "mobile-playback-after-external-urls.json"],
  ["mobile_billing_entitlement_flow.mjs", "mobile-billing-entitlement-after-external-urls.json"],
  ["mobile_revoke_restore_flow.mjs", "mobile-revoke-restore-after-external-urls.json"],
];

for (const [script, fileName] of fileMap) {
  const hit = rerun.results.find((item, index) => rerun.scripts[index] === script);
  const payload = hit
    ? { generatedAt: getNowIso(), script, status: hit.code === 0 ? "pass" : "fail", stdout: hit.stdout, stderr: hit.stderr }
    : { generatedAt: getNowIso(), script, status: "blocked", reason: rerun.reason };
  await writeJson(path.join(EVIDENCE_DIR, fileName), payload);
}

console.log(JSON.stringify(rerun, null, 2));
process.exit(rerun.rerunExecuted ? 0 : 1);
