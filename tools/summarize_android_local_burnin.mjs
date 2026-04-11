import path from "node:path";
import {
  EVIDENCE_DIR,
  getNowIso,
  readJsonIfExists,
  writeJson,
} from "./android_local_common.mjs";

const dockerReadiness = await readJsonIfExists(path.join(EVIDENCE_DIR, "docker-daemon-readiness.json"));
const dockerDiagnosis = await readJsonIfExists(path.join(EVIDENCE_DIR, "docker-runtime-diagnosis.json"));
const backendHealth = await readJsonIfExists(path.join(EVIDENCE_DIR, "local-backend-health-check.json"));
const emulator = await readJsonIfExists(path.join(EVIDENCE_DIR, "android-emulator-readiness.json"));
const debugTarget = await readJsonIfExists(path.join(EVIDENCE_DIR, "android-debug-target-check.json"));
const feedDetail = await readJsonIfExists(path.join(EVIDENCE_DIR, "android-feed-detail-smoke.json"));
const playback = await readJsonIfExists(path.join(EVIDENCE_DIR, "android-playback-smoke.json"));
const billing = await readJsonIfExists(path.join(EVIDENCE_DIR, "android-billing-entitlement-smoke.json"));
const restore = await readJsonIfExists(path.join(EVIDENCE_DIR, "android-revoke-restore-smoke.json"));

const appSmokeExecuted = [feedDetail, playback, billing, restore].some((item) => item?.status === "pass" || item?.status === "fail");
const payload = {
  generatedAt: getNowIso(),
  dockerReady: dockerReadiness?.ready ?? false,
  backendHealthy: backendHealth?.allHealthy ?? false,
  emulatorReady: emulator?.ready ?? false,
  debugTargetReady: debugTarget?.ready ?? false,
  appSmokeExecuted,
  feedDetailStatus: feedDetail?.status ?? "not_run",
  playbackStatus: playback?.status ?? "not_run",
  billingEntitlementStatus: billing?.status ?? "not_run",
  revokeRestoreStatus: restore?.status ?? "not_run",
  currentState: appSmokeExecuted
    ? "local_app_smoke_executed"
    : emulator?.ready
      ? "ready_for_manual_app_smoke"
      : "local_app_smoke_blocked_by_emulator_assets",
  blockerSummary: emulator?.ready
    ? "Emulator/device is ready. Proceed with real App walkthrough."
    : emulator?.blockerSummary ?? "App smoke is blocked before device boot.",
  dockerRuntimeImpact: {
    blocksAppSmoke: false,
    blocksComposeObservability: true,
    rootCause: dockerDiagnosis?.rootCause ?? "Docker runtime diagnosis unavailable.",
  },
};

await writeJson(path.join(EVIDENCE_DIR, "android-local-app-burnin-summary.json"), payload);

const hardening = {
  generatedAt: getNowIso(),
  dockerReady: dockerReadiness?.ready ?? false,
  composeObservable: false,
  rootCause: dockerDiagnosis?.rootCause ?? "Docker runtime diagnosis unavailable.",
  minimalRepairActions: [
    "Repair Docker Desktop named-pipe handshake so docker info and docker ps return normally.",
    "Reopen Docker Desktop or restart com.docker.service after preserving currently healthy local services.",
    "Re-verify docker info, docker ps, and docker compose ps after runtime repair.",
  ],
  impact: {
    blocksAppSmoke: false,
    blocksLocalScriptedIntegration: false,
    blocksStagingValidation: false,
    blocksLocalRuntimeObservability: true,
  },
};
await writeJson(path.join(EVIDENCE_DIR, "docker-runtime-hardening-status.json"), hardening);

console.log(JSON.stringify({ payload, hardening }, null, 2));
process.exit(appSmokeExecuted ? 0 : 1);
