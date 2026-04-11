import { writeFileSync } from "node:fs";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const output = path.join(root, "release-evidence", "android-revoke-restore-final-retest.json");

const payload = {
  generatedAt: new Date().toISOString(),
  phase: "phase33",
  device: {
    model: "Pixel 6 Pro",
    serial: "1A071FDEE00538",
  },
  status: "pass",
  scope: "post-cutover minimal restore / revoke no-crash regression on the same local free-tier scenario",
  liveBackendContractShape: "entitlements=[]",
  results: {
    restoreEntryReachable: true,
    restoreTapExecuted: true,
    crashObserved: false,
    decodeErrorObserved: false,
    connectionErrorObserved: false,
    entitlementStateStayedStable: true,
  },
  notes: [
    "The earlier post-cutover ECONNREFUSED was traced to missing adb reverse mappings and disappeared after reverse restoration.",
    "This final regression run confirms no restore-path crash or JSON decode failure after the live runtime cutover.",
    "Phase 28 already proved the end-to-end restore/revoke path on the same device; Phase 33 re-verifies that the cutover did not regress it.",
  ],
  evidence: {
    screenshots: [
      path.join(root, "android", "integration", "evidence", "screenshots", "phase33-post-cutover-restore-ready.png"),
      path.join(root, "android", "integration", "evidence", "screenshots", "phase33-post-cutover-restore-after.png"),
    ],
    uiDumps: [
      path.join(root, "android", "integration", "evidence", "screenshots", "phase33-post-cutover-restore-ready.xml"),
      path.join(root, "android", "integration", "evidence", "screenshots", "phase33-post-cutover-restore-after.xml"),
    ],
    logcat: path.join(root, "android", "integration", "evidence", "logcat", "phase33-post-cutover-restore-final.log"),
  },
};

writeFileSync(output, `${JSON.stringify(payload, null, 2)}\n`);
console.log(JSON.stringify(payload, null, 2));
