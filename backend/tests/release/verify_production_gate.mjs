import fs from "node:fs";
import path from "node:path";

function requireEnv(flag, reason) {
  if (process.env[flag] !== "1") {
    return reason;
  }
  return null;
}

function hasGitMetadata(root) {
  return fs.existsSync(path.join(root, ".git"));
}

async function main() {
  const root = path.resolve(import.meta.dirname, "..", "..", "..");
  const blockers = [];

  if (!hasGitMetadata(root)) {
    blockers.push("workspace has no .git metadata; candidate commit sha cannot be proven");
  }

  for (const [file, label] of [
    ["docs/staging-rehearsal-report.md", "staging rehearsal report"],
    ["docs/canary-drill-report.md", "canary drill report"],
    ["docs/rollback-drill-report.md", "rollback drill report"],
    ["docs/soak-test-report.md", "soak report"],
    ["docs/release-evidence-pack.md", "release evidence pack"],
    ["docs/production-go-no-go.md", "production go/no-go"],
    ["docs/release-blockers-final.md", "final blocker list"],
    ["docs/release-candidate-manifest.md", "candidate manifest"]
  ]) {
    if (!fs.existsSync(path.join(root, file))) {
      blockers.push(`missing ${label}`);
    }
  }

  for (const issue of [
    requireEnv("DRAMAFLOW_STAGING_REHEARSAL_SIGNED", "staging rehearsal sign-off missing"),
    requireEnv("DRAMAFLOW_CANARY_DRILL_SIGNED", "canary drill sign-off missing"),
    requireEnv("DRAMAFLOW_ROLLBACK_DRILL_SIGNED", "rollback drill sign-off missing"),
    requireEnv("DRAMAFLOW_SOAK_EVIDENCE_SIGNED", "soak evidence sign-off missing"),
    requireEnv("DRAMAFLOW_PRODUCTION_APPROVERS_READY", "production approver chain not confirmed")
  ]) {
    if (issue) {
      blockers.push(issue);
    }
  }

  if (blockers.length > 0) {
    console.error("[production-gate] no-go");
    for (const blocker of blockers) {
      console.error(`- ${blocker}`);
    }
    process.exit(1);
  }

  console.log("[production-gate] go");
}

main().catch((error) => {
  console.error(`[production-gate] fail: ${error.message}`);
  process.exit(1);
});
