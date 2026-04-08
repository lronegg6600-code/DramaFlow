import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

function run(command, args, cwd) {
  try {
    return execFileSync(command, args, {
      cwd,
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"]
    }).trim();
  } catch (error) {
    return null;
  }
}

function boolEnv(name) {
  return process.env[name] === "1";
}

async function main() {
  const root = path.resolve(import.meta.dirname, "..", "..", "..");
  const evidenceDir = process.env.DRAMAFLOW_EVIDENCE_DIR
    ? path.resolve(process.env.DRAMAFLOW_EVIDENCE_DIR)
    : path.join(root, "release-evidence");

  fs.mkdirSync(evidenceDir, { recursive: true });

  const gitRoot = run("git", ["rev-parse", "--show-toplevel"], root);
  const commitSha = gitRoot ? run("git", ["rev-parse", "HEAD"], root) : null;
  const branch = gitRoot ? run("git", ["rev-parse", "--abbrev-ref", "HEAD"], root) : null;
  const tag = gitRoot ? run("git", ["describe", "--tags", "--exact-match"], root) : null;
  const changedFiles = gitRoot
    ? (run("git", ["diff", "--name-only", "HEAD~1", "HEAD"], root) ?? "")
        .split(/\r?\n/)
        .filter(Boolean)
    : [];

  const kubectlPath = run("where.exe", ["kubectl"], root);
  const kubeContext = kubectlPath ? run("kubectl", ["config", "current-context"], root) : null;
  const kubeContexts = kubectlPath ? run("kubectl", ["config", "get-contexts", "-o=name"], root) : null;

  const metadata = {
    generatedAt: new Date().toISOString(),
    workspace: root,
    git: {
      available: Boolean(gitRoot),
      root: gitRoot,
      commitSha,
      branch,
      tag,
      changedFiles
    },
    github: {
      runId: process.env.GITHUB_RUN_ID ?? null,
      runNumber: process.env.GITHUB_RUN_NUMBER ?? null,
      workflow: process.env.GITHUB_WORKFLOW ?? null,
      ref: process.env.GITHUB_REF ?? null,
      sha: process.env.GITHUB_SHA ?? null
    },
    deploy: {
      targetEnvironment: process.env.DRAMAFLOW_TARGET_ENV ?? null,
      controlPlane: process.env.DRAMAFLOW_CONTROL_PLANE ?? null,
      dryRun: !boolEnv("DRAMAFLOW_REAL_RUN"),
      kubectlAvailable: Boolean(kubectlPath),
      kubeContext,
      kubeContexts: kubeContexts ? kubeContexts.split(/\r?\n/).filter(Boolean) : [],
      namespace: process.env.DRAMAFLOW_KUBE_NAMESPACE ?? null,
      imageTag: process.env.DRAMAFLOW_IMAGE_TAG ?? null,
      imageDigest: process.env.DRAMAFLOW_IMAGE_DIGEST ?? null,
      adminBuildArtifact: process.env.DRAMAFLOW_ADMIN_BUILD_ARTIFACT ?? null
    },
    blockers: []
  };

  if (!metadata.git.available) {
    metadata.blockers.push("workspace has no .git metadata; commit SHA and tag cannot be proven");
  }
  if (!metadata.deploy.kubectlAvailable) {
    metadata.blockers.push("kubectl is not available");
  }
  if (metadata.deploy.kubectlAvailable && !metadata.deploy.kubeContext) {
    metadata.blockers.push("kubectl has no current context; real cluster rehearsal cannot execute");
  }
  if (!metadata.deploy.imageTag) {
    metadata.blockers.push("image tag is not set");
  }

  const outFile = path.join(evidenceDir, "release-metadata.json");
  fs.writeFileSync(outFile, `${JSON.stringify(metadata, null, 2)}\n`, "utf8");
  console.log(outFile);
}

main().catch((error) => {
  console.error(`[collect-release-metadata] fail: ${error.message}`);
  process.exit(1);
});
