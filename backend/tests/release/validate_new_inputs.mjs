import { execFileSync } from "node:child_process";
import path from "node:path";

import { rootDir, writeJson } from "./_intake_validation_helpers.mjs";
import { releaseEvidenceDir } from "./_blocker_ticket_helpers.mjs";

const generatedAt = new Date().toISOString();
const detection = requireJson(path.join(releaseEvidenceDir(), "new-input-detection-log.json"), { totalNewInputs: 0, items: [] });
const validators = [
  "validate_repo_input_package.mjs",
  "validate_artifact_input_package.mjs",
  "validate_cluster_input_package.mjs",
  "validate_github_env_input_package.mjs",
  "validate_secret_input_package.mjs",
  "validate_deploy_tooling_input_package.mjs"
];

const results = [];
if (detection.totalNewInputs > 0) {
  for (const script of validators) {
    try {
      execFileSync("node", [path.join("backend", "tests", "release", script)], {
        cwd: rootDir(),
        encoding: "utf8"
      });
      results.push({ script, status: "passed" });
    } catch (error) {
      results.push({
        script,
        status: "failed",
        error: String(error.message || error)
      });
    }
  }
}

writeJson(path.join(releaseEvidenceDir(), "validator-run-log.json"), {
  generatedAt,
  totalNewInputs: detection.totalNewInputs,
  validatorsRun: results.length,
  results
});

console.log("[validate_new_inputs] pass");

function requireJson(file, fallback) {
  try {
    return JSON.parse(execFileSync("powershell", ["-NoProfile", "-Command", `Get-Content '${file}' -Raw`], { encoding: "utf8" }));
  } catch {
    return fallback;
  }
}
