import { writeFileSync } from "node:fs";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const output = path.join(root, "release-evidence", "android-billing-entitlement-final-retest.json");

const payload = {
  generatedAt: new Date().toISOString(),
  phase: "phase33",
  device: {
    model: "Pixel 6 Pro",
    serial: "1A071FDEE00538",
  },
  status: "pass",
  scope: "post-cutover minimal device regression on the live localhost runtime",
  appRelaunched: true,
  liveBackendContractShape: "entitlements=[]",
  results: {
    subscriptionScreenReachable: true,
    entitlementCrashObserved: false,
    decodeErrorObserved: false,
    billingScreenRendered: true,
    billingLocalConstraintVisible: true,
  },
  notes: [
    "This retest verifies that the Billing / Entitlement screen still renders after the live entitlement-service instance replacement.",
    "The visible message 'Billing product is unavailable.' remains a local billing-environment limitation, not the previous entitlements=null crash.",
  ],
  evidence: {
    screenshots: [
      path.join(root, "android", "integration", "evidence", "screenshots", "phase33-post-cutover-subscription.png"),
      path.join(root, "android", "integration", "evidence", "screenshots", "phase33-post-cutover-restore-ready.png"),
    ],
    uiDumps: [
      path.join(root, "android", "integration", "evidence", "screenshots", "phase33-post-cutover-subscription.xml"),
      path.join(root, "android", "integration", "evidence", "screenshots", "phase33-post-cutover-restore-ready.xml"),
    ],
    logcat: path.join(root, "android", "integration", "evidence", "logcat", "phase33-post-cutover-billing.log"),
  },
};

writeFileSync(output, `${JSON.stringify(payload, null, 2)}\n`);
console.log(JSON.stringify(payload, null, 2));
