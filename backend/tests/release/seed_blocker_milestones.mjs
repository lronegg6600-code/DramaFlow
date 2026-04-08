import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

import { ghExecutable } from "./_blocker_ticket_helpers.mjs";
import { rootDir, writeJson } from "./_intake_validation_helpers.mjs";

const sourceFile = path.join(rootDir(), ".github", "labels", "blocker-milestones.json");
const payload = JSON.parse(fs.readFileSync(sourceFile, "utf8"));
const outputFile = path.join(rootDir(), "release-evidence", "blocker-milestone-seed.json");
const gh = ghExecutable();
const repo = process.env.GITHUB_REPOSITORY;
const mode = String(process.env.DRAMAFLOW_LIVE_SEED || "false").toLowerCase() === "true" ? "real" : "seed_only";
const created = [];
const skipped = [];
const failures = [];

if (mode === "real" && repo) {
  for (const milestone of payload.milestones) {
    try {
      execFileSync(
        gh,
        [
          "api",
          `repos/${repo}/milestones`,
          "--method",
          "POST",
          "-f",
          `title=${milestone.title}`,
          "-f",
          `description=${milestone.description}`,
          "-f",
          `state=${milestone.state}`
        ],
        {
          encoding: "utf8",
          env: { ...process.env, HTTP_PROXY: "", HTTPS_PROXY: "", ALL_PROXY: "" }
        }
      );
      created.push(milestone.title);
    } catch (error) {
      try {
        execFileSync(
          gh,
          [
            "api",
            `repos/${repo}/milestones?state=all`
          ],
          {
            encoding: "utf8",
            env: { ...process.env, HTTP_PROXY: "", HTTPS_PROXY: "", ALL_PROXY: "" }
          }
        );
        skipped.push(milestone.title);
      } catch {
        failures.push({ milestone: milestone.title, reason: String(error.message || error) });
      }
    }
  }
} else {
  skipped.push("live milestone creation skipped");
}

writeJson(outputFile, {
  generatedAt: new Date().toISOString(),
  sourceFile,
  total: payload.milestones.length,
  mode,
  repo: repo || null,
  milestones: payload.milestones,
  created,
  skipped,
  failures
});

console.log("[seed_blocker_milestones] pass");
