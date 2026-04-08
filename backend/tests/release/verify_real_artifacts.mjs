import path from "node:path";
import { evidenceDir, item, printAndExit, summarize, writeJson } from "./_real_input_helpers.mjs";

async function main() {
  const outFile = path.join(evidenceDir(), "real-artifact-identity.json");

  const imageTag = process.env.DRAMAFLOW_IMAGE_TAG ?? null;
  const imageDigest = process.env.DRAMAFLOW_IMAGE_DIGEST ?? null;
  const adminArtifact = process.env.DRAMAFLOW_ADMIN_BUILD_ARTIFACT ?? null;
  const workflowRunId = process.env.GITHUB_RUN_ID ?? null;
  const workflowSha = process.env.GITHUB_SHA ?? null;
  const registry = process.env.DRAMAFLOW_REGISTRY ?? null;

  const items = [
    item("docker image tag", imageTag ? "verified" : "missing", imageTag ?? "DRAMAFLOW_IMAGE_TAG not set", "CI / platform"),
    item("docker image digest", imageDigest ? "verified" : "missing", imageDigest ?? "DRAMAFLOW_IMAGE_DIGEST not set", "CI / platform"),
    item("admin web artifact id", adminArtifact ? "verified" : "missing", adminArtifact ?? "DRAMAFLOW_ADMIN_BUILD_ARTIFACT not set", "CI / admin owner"),
    item("workflow run id", workflowRunId ? "verified" : "missing", workflowRunId ?? "GITHUB_RUN_ID not set", "repo admin / CI"),
    item("workflow sha", workflowSha ? "verified" : "missing", workflowSha ?? "GITHUB_SHA not set", "repo admin / CI"),
    item("registry identity", registry ? "verified" : "missing", registry ?? "DRAMAFLOW_REGISTRY not set", "platform / registry admin")
  ];

  const payload = {
    category: "artifact_identity",
    generatedAt: new Date().toISOString(),
    items,
    summary: summarize(items),
    values: { imageTag, imageDigest, adminArtifact, workflowRunId, workflowSha, registry }
  };

  writeJson(outFile, payload);
  printAndExit("verify-real-artifacts", payload);
}

main().catch((error) => {
  console.error(`[verify-real-artifacts] fail: ${error.message}`);
  process.exit(1);
});
