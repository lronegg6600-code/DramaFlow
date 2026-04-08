import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

import { ghExecutable, loadOwnerMatrix, slugify } from "./_blocker_ticket_helpers.mjs";
import { rootDir, writeJson } from "./_intake_validation_helpers.mjs";

const sourceFile = path.join(rootDir(), ".github", "labels", "blocker-labels.json");
const payload = JSON.parse(fs.readFileSync(sourceFile, "utf8"));
const blockers = loadOwnerMatrix();
const outputFile = path.join(rootDir(), "release-evidence", "blocker-label-seed.json");

const dynamicLabels = [
  ...new Set(blockers.map((item) => `severity:${String(item.severity).toLowerCase()}`)),
  ...new Set(blockers.map((item) => `owner:${slugify(item.primary_owner)}`)),
  ...new Set(blockers.map((item) => `provider:${slugify(item.provider_role)}`)),
  ...new Set(blockers.map((item) => `sla:${slugify(item.sla_level)}`))
].map((name) => ({
  name,
  color: "C2E0C6",
  description: "Generated launch metadata label"
}));

const labels = [...payload.labels, ...dynamicLabels];

const gh = ghExecutable();
const repo = process.env.GITHUB_REPOSITORY;
const mode = String(process.env.DRAMAFLOW_LIVE_SEED || "false").toLowerCase() === "true" ? "real" : "seed_only";
const created = [];
const skipped = [];
const failures = [];

if (mode === "real" && repo) {
  for (const label of labels) {
    try {
      execFileSync(
        gh,
        ["label", "create", label.name, "--repo", repo, "--color", label.color, "--description", label.description, "--force"],
        {
          encoding: "utf8",
          env: { ...process.env, HTTP_PROXY: "", HTTPS_PROXY: "", ALL_PROXY: "" }
        }
      );
      created.push(label.name);
    } catch (error) {
      failures.push({ label: label.name, reason: String(error.message || error) });
    }
  }
} else {
  skipped.push("live label creation skipped");
}

writeJson(outputFile, {
  generatedAt: new Date().toISOString(),
  sourceFile,
  total: labels.length,
  mode,
  repo: repo || null,
  labels,
  created,
  skipped,
  failures
});

console.log("[seed_blocker_labels] pass");
