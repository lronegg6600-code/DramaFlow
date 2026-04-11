import { writeFileSync } from "node:fs";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const output = path.join(root, "release-evidence", "android-post-cutover-evidence-index.json");

const payload = {
  generatedAt: new Date().toISOString(),
  phase: "phase33",
  device: {
    model: "Pixel 6 Pro",
    serial: "1A071FDEE00538",
  },
  screenshots: [
    path.join(root, "android", "integration", "evidence", "screenshots", "phase33-post-cutover-subscription.png"),
    path.join(root, "android", "integration", "evidence", "screenshots", "phase33-post-cutover-restore-ready.png"),
    path.join(root, "android", "integration", "evidence", "screenshots", "phase33-post-cutover-restore-after.png"),
  ],
  uiDumps: [
    path.join(root, "android", "integration", "evidence", "screenshots", "phase33-post-cutover-subscription.xml"),
    path.join(root, "android", "integration", "evidence", "screenshots", "phase33-post-cutover-restore-ready.xml"),
    path.join(root, "android", "integration", "evidence", "screenshots", "phase33-post-cutover-restore-after.xml"),
  ],
  logcat: [
    path.join(root, "android", "integration", "evidence", "logcat", "phase33-post-cutover-billing.log"),
    path.join(root, "android", "integration", "evidence", "logcat", "phase33-post-cutover-restore-final.log"),
  ],
  priorSupportingEvidence: [
    path.join(root, "android", "integration", "evidence", "screenshots", "phase28-subscription-top.png"),
    path.join(root, "android", "integration", "evidence", "screenshots", "phase28-restore-after-fix.png"),
    path.join(root, "android", "integration", "evidence", "screenshots", "phase28-post-revoke-home.png"),
  ],
};

writeFileSync(output, `${JSON.stringify(payload, null, 2)}\n`);
console.log(JSON.stringify(payload, null, 2));
