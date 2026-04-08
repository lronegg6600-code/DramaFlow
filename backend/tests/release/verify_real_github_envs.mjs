import path from "node:path";
import { evidenceDir, item, printAndExit, summarize, writeJson } from "./_real_input_helpers.mjs";

function flag(name) {
  return process.env[name] === "1";
}

async function main() {
  const outFile = path.join(evidenceDir(), "real-github-environments.json");

  const items = [
    item("staging environment", flag("DRAMAFLOW_GITHUB_STAGING_ENV_READY") ? "verified" : "missing", flag("DRAMAFLOW_GITHUB_STAGING_ENV_READY") ? "confirmed" : "not confirmed", "repo admin / release manager", true, false),
    item("production environment", flag("DRAMAFLOW_GITHUB_PRODUCTION_ENV_READY") ? "verified" : "missing", flag("DRAMAFLOW_GITHUB_PRODUCTION_ENV_READY") ? "confirmed" : "not confirmed", "repo admin / release manager", false, true),
    item("required reviewers", flag("DRAMAFLOW_GITHUB_REQUIRED_REVIEWERS_READY") ? "verified" : "missing", flag("DRAMAFLOW_GITHUB_REQUIRED_REVIEWERS_READY") ? "confirmed" : "not confirmed", "repo admin / release manager", false, true),
    item("prevent self-review", flag("DRAMAFLOW_GITHUB_PREVENT_SELF_REVIEW") ? "verified" : "missing", flag("DRAMAFLOW_GITHUB_PREVENT_SELF_REVIEW") ? "confirmed" : "not confirmed", "repo admin / release manager", false, true),
    item("deployment branch/tag policy", flag("DRAMAFLOW_GITHUB_DEPLOYMENT_POLICY_READY") ? "verified" : "missing", flag("DRAMAFLOW_GITHUB_DEPLOYMENT_POLICY_READY") ? "confirmed" : "not confirmed", "repo admin / release manager", false, true),
    item("artifact/evidence upload policy", flag("DRAMAFLOW_GITHUB_EVIDENCE_ARTIFACTS_READY") ? "verified" : "missing", flag("DRAMAFLOW_GITHUB_EVIDENCE_ARTIFACTS_READY") ? "confirmed" : "not confirmed", "repo admin / release manager", true, true)
  ];

  const payload = {
    category: "github_release_control",
    generatedAt: new Date().toISOString(),
    items,
    summary: summarize(items),
    values: {
      stagingReady: flag("DRAMAFLOW_GITHUB_STAGING_ENV_READY"),
      productionReady: flag("DRAMAFLOW_GITHUB_PRODUCTION_ENV_READY"),
      reviewersReady: flag("DRAMAFLOW_GITHUB_REQUIRED_REVIEWERS_READY"),
      preventSelfReview: flag("DRAMAFLOW_GITHUB_PREVENT_SELF_REVIEW"),
      policyReady: flag("DRAMAFLOW_GITHUB_DEPLOYMENT_POLICY_READY"),
      evidenceArtifactsReady: flag("DRAMAFLOW_GITHUB_EVIDENCE_ARTIFACTS_READY")
    }
  };

  writeJson(outFile, payload);
  printAndExit("verify-real-github-envs", payload);
}

main().catch((error) => {
  console.error(`[verify-real-github-envs] fail: ${error.message}`);
  process.exit(1);
});
