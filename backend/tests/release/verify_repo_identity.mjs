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
  } catch {
    return null;
  }
}

function writeResult(file, payload) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, `${JSON.stringify(payload, null, 2)}\n`, "utf8");
}

async function main() {
  const root = path.resolve(import.meta.dirname, "..", "..", "..");
  const evidenceDir = process.env.DRAMAFLOW_EVIDENCE_DIR
    ? path.resolve(process.env.DRAMAFLOW_EVIDENCE_DIR)
    : path.join(root, "release-evidence");
  const outFile = path.join(evidenceDir, "repo-identity.json");

  const gitRoot = run("git", ["rev-parse", "--show-toplevel"], root);
  const commitSha = gitRoot ? run("git", ["rev-parse", "HEAD"], root) : null;
  const branch = gitRoot ? run("git", ["rev-parse", "--abbrev-ref", "HEAD"], root) : null;
  const tag = gitRoot ? run("git", ["describe", "--tags", "--exact-match"], root) : null;
  const changedFiles = gitRoot
    ? (run("git", ["status", "--short"], root) ?? "").split(/\r?\n/).filter(Boolean)
    : [];
  const candidateVersion = process.env.DRAMAFLOW_RELEASE_VERSION ?? null;

  const blockers = [];
  if (!gitRoot) blockers.push("workspace has no .git metadata");
  if (!commitSha) blockers.push("commit SHA is not provable");
  if (!branch) blockers.push("branch is not provable");
  if (!tag && !candidateVersion) blockers.push("tag/version is not provided");

  const result = {
    category: "repository_identity",
    status: blockers.length === 0 ? "pass" : "blocker",
    generatedAt: new Date().toISOString(),
    git: { root: gitRoot, commitSha, branch, tag, changedFiles },
    candidateVersion,
    blockers
  };

  writeResult(outFile, result);

  if (blockers.length > 0) {
    console.error("[verify-repo-identity] blocker");
    for (const blocker of blockers) console.error(`- ${blocker}`);
    process.exit(1);
  }

  console.log("[verify-repo-identity] pass");
}

main().catch((error) => {
  console.error(`[verify-repo-identity] fail: ${error.message}`);
  process.exit(1);
});
