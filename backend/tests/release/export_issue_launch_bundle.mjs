import path from "node:path";

import { readJsonIfExists, releaseEvidenceDir, writeJson } from "./_blocker_ticket_helpers.mjs";

const evidenceDir = releaseEvidenceDir();
const payloads = readJsonIfExists(path.join(evidenceDir, "blocker-issue-launch-payloads.json"), { items: [] });
const labels = readJsonIfExists(path.join(evidenceDir, "blocker-label-seed.json"), { labels: [] });
const milestones = readJsonIfExists(path.join(evidenceDir, "blocker-milestone-seed.json"), { milestones: [] });
const project = readJsonIfExists(path.join(evidenceDir, "blocker-project-seed.json"), {});

writeJson(path.join(evidenceDir, "blocker-launch-bundle.json"), {
  generatedAt: new Date().toISOString(),
  payloads,
  labels,
  milestones,
  project
});

console.log("[export_issue_launch_bundle] pass");
