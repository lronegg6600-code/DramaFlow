import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, runLocalMobileScripts, writeJson } from "./local_docker_common.mjs";

const dockerReadiness = await readJsonIfExists(path.join(EVIDENCE_DIR, "docker-daemon-readiness.json"));
const portCheck = await readJsonIfExists(path.join(EVIDENCE_DIR, "local-backend-port-check.json"));
const healthCheck = await readJsonIfExists(path.join(EVIDENCE_DIR, "local-backend-health-check.json"));

let payload;
if (!portCheck?.allReachable || !healthCheck?.allHealthy) {
  payload = {
    generatedAt: getNowIso(),
    rerunExecuted: false,
    scripts: [
      "mobile_backend_contract_smoke.mjs",
      "mobile_auth_feed_detail_flow.mjs",
      "mobile_playback_session_flow.mjs",
      "mobile_billing_entitlement_flow.mjs",
      "mobile_revoke_restore_flow.mjs",
    ],
    results: [],
    reason: "Skipped due to local backend runtime not ready.",
  };
} else {
  const rerun = await runLocalMobileScripts();
  payload = {
    generatedAt: getNowIso(),
    rerunExecuted: true,
    scripts: rerun.scripts,
    results: rerun.results,
    reason: dockerReadiness?.ready
      ? "Local backend runtime ready; mobile local integration rerun executed."
      : "Local services are healthy even though Docker CLI remains degraded; mobile local integration rerun executed.",
  };
}

await writeJson(path.join(EVIDENCE_DIR, "mobile-rerun-with-local-backend.json"), payload);

const artifactMap = [
  ["mobile_backend_contract_smoke.mjs", "mobile-contract-smoke-local.json"],
  ["mobile_auth_feed_detail_flow.mjs", "mobile-auth-feed-detail-local.json"],
  ["mobile_playback_session_flow.mjs", "mobile-playback-local.json"],
  ["mobile_billing_entitlement_flow.mjs", "mobile-billing-entitlement-local.json"],
  ["mobile_revoke_restore_flow.mjs", "mobile-revoke-restore-local.json"],
];
for (const [script, fileName] of artifactMap) {
  const result = payload.results.find((item) => item.script === script);
  const itemPayload = {
    generatedAt: getNowIso(),
    script,
    status: payload.rerunExecuted ? (result?.code === 0 ? "pass" : "fail") : "blocked",
    reason: payload.rerunExecuted ? (result?.stderr || result?.stdout || "Executed.") : payload.reason,
  };
  await writeJson(path.join(EVIDENCE_DIR, fileName), itemPayload);
}

console.log(JSON.stringify(payload, null, 2));
process.exit(payload.rerunExecuted ? 0 : 1);
