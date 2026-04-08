import fs from "node:fs";
import path from "node:path";

function writeResult(file, payload) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, `${JSON.stringify(payload, null, 2)}\n`, "utf8");
}

async function main() {
  const root = path.resolve(import.meta.dirname, "..", "..", "..");
  const evidenceDir = process.env.DRAMAFLOW_EVIDENCE_DIR
    ? path.resolve(process.env.DRAMAFLOW_EVIDENCE_DIR)
    : path.join(root, "release-evidence");
  const outFile = path.join(evidenceDir, "artifact-identity.json");

  const values = {
    imageTag: process.env.DRAMAFLOW_IMAGE_TAG ?? null,
    imageDigest: process.env.DRAMAFLOW_IMAGE_DIGEST ?? null,
    adminBuildArtifact: process.env.DRAMAFLOW_ADMIN_BUILD_ARTIFACT ?? null,
    workflowRunId: process.env.GITHUB_RUN_ID ?? null,
    workflowRunNumber: process.env.GITHUB_RUN_NUMBER ?? null,
    workflowName: process.env.GITHUB_WORKFLOW ?? null,
    githubSha: process.env.GITHUB_SHA ?? null,
    registry: process.env.DRAMAFLOW_REGISTRY ?? null
  };

  const blockers = [];
  if (!values.imageTag) blockers.push("docker image tag is missing");
  if (!values.imageDigest) blockers.push("docker image digest is missing");
  if (!values.adminBuildArtifact) blockers.push("admin build artifact id is missing");
  if (!values.workflowRunId) blockers.push("workflow run id is missing");
  if (!values.githubSha) blockers.push("workflow github sha is missing");
  if (!values.registry) blockers.push("registry identity is missing");

  const result = {
    category: "artifact_identity",
    status: blockers.length === 0 ? "pass" : "blocker",
    generatedAt: new Date().toISOString(),
    values,
    blockers
  };

  writeResult(outFile, result);

  if (blockers.length > 0) {
    console.error("[verify-artifact-identity] blocker");
    for (const blocker of blockers) console.error(`- ${blocker}`);
    process.exit(1);
  }

  console.log("[verify-artifact-identity] pass");
}

main().catch((error) => {
  console.error(`[verify-artifact-identity] fail: ${error.message}`);
  process.exit(1);
});
