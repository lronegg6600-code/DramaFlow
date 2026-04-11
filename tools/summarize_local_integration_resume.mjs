import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./local_docker_common.mjs";

const dockerReadiness = await readJsonIfExists(path.join(EVIDENCE_DIR, "docker-daemon-readiness.json"));
const diagnosis = await readJsonIfExists(path.join(EVIDENCE_DIR, "docker-runtime-diagnosis.json"));
const stack = await readJsonIfExists(path.join(EVIDENCE_DIR, "docker-compose-stack-status.json"));
const ports = await readJsonIfExists(path.join(EVIDENCE_DIR, "local-backend-port-check.json"));
const health = await readJsonIfExists(path.join(EVIDENCE_DIR, "local-backend-health-check.json"));
const envExport = await readJsonIfExists(path.join(EVIDENCE_DIR, "android-local-debug-env-export.json"));
const rerun = await readJsonIfExists(path.join(EVIDENCE_DIR, "mobile-rerun-with-local-backend.json"));

const payload = {
  generatedAt: getNowIso(),
  dockerReady: dockerReadiness?.ready ?? false,
  composeReachable: stack?.started ?? stack?.ready ?? false,
  allPortsReachable: ports?.allReachable ?? false,
  allHealthChecksPassing: health?.allHealthy ?? false,
  localDebugEnvExported: envExport?.exported ?? false,
  rerunExecuted: rerun?.rerunExecuted ?? false,
  currentState: rerun?.rerunExecuted
    ? "local_integration_rerun_executed"
    : ports?.allReachable && health?.allHealthy
      ? "partially_unblocked"
      : dockerReadiness?.ready
        ? "partially_unblocked"
        : "still_blocked_by_local_docker_runtime",
  runtimeRootCause: diagnosis?.rootCause ?? "Docker runtime not diagnosed.",
};

await writeJson(path.join(EVIDENCE_DIR, "mobile-integration-resume-summary.json"), payload);
console.log(JSON.stringify(payload, null, 2));
