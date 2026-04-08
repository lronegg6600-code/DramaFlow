import fs from "node:fs";
import path from "node:path";

function writeResult(file, payload) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, `${JSON.stringify(payload, null, 2)}\n`, "utf8");
}

function envFlag(name) {
  return process.env[name] === "1";
}

async function main() {
  const root = path.resolve(import.meta.dirname, "..", "..", "..");
  const evidenceDir = process.env.DRAMAFLOW_EVIDENCE_DIR
    ? path.resolve(process.env.DRAMAFLOW_EVIDENCE_DIR)
    : path.join(root, "release-evidence");
  const outFile = path.join(evidenceDir, "github-environments.json");

  const values = {
    stagingConfigured: envFlag("DRAMAFLOW_GITHUB_STAGING_ENV_READY"),
    productionConfigured: envFlag("DRAMAFLOW_GITHUB_PRODUCTION_ENV_READY"),
    requiredReviewersConfigured: envFlag("DRAMAFLOW_GITHUB_REQUIRED_REVIEWERS_READY"),
    preventSelfReview: envFlag("DRAMAFLOW_GITHUB_PREVENT_SELF_REVIEW"),
    branchPolicyConfigured: envFlag("DRAMAFLOW_GITHUB_DEPLOYMENT_POLICY_READY"),
    evidenceArtifactsConfigured: envFlag("DRAMAFLOW_GITHUB_EVIDENCE_ARTIFACTS_READY")
  };

  const blockers = [];
  if (!values.stagingConfigured) blockers.push("GitHub staging environment is not confirmed");
  if (!values.productionConfigured) blockers.push("GitHub production environment is not confirmed");
  if (!values.requiredReviewersConfigured) blockers.push("required reviewers are not confirmed");
  if (!values.preventSelfReview) blockers.push("prevent self-review policy is not confirmed");
  if (!values.branchPolicyConfigured) blockers.push("deployment branch/tag policy is not confirmed");
  if (!values.evidenceArtifactsConfigured) blockers.push("artifact/evidence upload policy is not confirmed");

  const result = {
    category: "github_release_control",
    status: blockers.length === 0 ? "pass" : "blocker",
    generatedAt: new Date().toISOString(),
    values,
    blockers
  };

  writeResult(outFile, result);

  if (blockers.length > 0) {
    console.error("[verify-github-environments] blocker");
    for (const blocker of blockers) console.error(`- ${blocker}`);
    process.exit(1);
  }

  console.log("[verify-github-environments] pass");
}

main().catch((error) => {
  console.error(`[verify-github-environments] fail: ${error.message}`);
  process.exit(1);
});
