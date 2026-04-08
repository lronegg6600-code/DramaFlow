import path from "node:path";
import { evidenceDir, item, printAndExit, rootDir, run, summarize, writeJson } from "./_real_input_helpers.mjs";

async function main() {
  const root = rootDir();
  const outFile = path.join(evidenceDir(), "real-repo-identity.json");

  const gitRoot = run("git", ["rev-parse", "--show-toplevel"], root);
  const commitSha = gitRoot ? run("git", ["rev-parse", "HEAD"], root) : null;
  const branch = gitRoot ? run("git", ["rev-parse", "--abbrev-ref", "HEAD"], root) : null;
  const tag = gitRoot ? run("git", ["describe", "--tags", "--exact-match"], root) : null;
  const releaseVersion = process.env.DRAMAFLOW_RELEASE_VERSION ?? null;
  const changedFiles = gitRoot ? (run("git", ["diff", "--name-only", "HEAD~1", "HEAD"], root) ?? "").split(/\r?\n/).filter(Boolean) : [];

  const items = [
    item(".git checkout", gitRoot ? "verified" : "missing", gitRoot ? gitRoot : "workspace has no git root", "repo owner / release manager"),
    item("commit SHA", commitSha ? "verified" : "missing", commitSha ?? "git rev-parse HEAD unavailable", "repo owner / release manager"),
    item("branch", branch ? "verified" : "missing", branch ?? "git branch unavailable", "repo owner / release manager"),
    item("tag or RC version", tag || releaseVersion ? "verified" : "missing", tag ?? releaseVersion ?? "tag/version unavailable", "release manager"),
    item("changed files summary", changedFiles.length > 0 ? "verified" : gitRoot ? "invalid" : "missing", changedFiles.length > 0 ? `${changedFiles.length} changed files captured` : gitRoot ? "git present but changed file summary empty" : "git unavailable", "repo owner / release manager", false, false)
  ];

  const payload = {
    category: "repository_identity",
    generatedAt: new Date().toISOString(),
    items,
    summary: summarize(items),
    values: { gitRoot, commitSha, branch, tag, releaseVersion, changedFiles }
  };

  writeJson(outFile, payload);
  printAndExit("verify-real-repo-identity", payload);
}

main().catch((error) => {
  console.error(`[verify-real-repo-identity] fail: ${error.message}`);
  process.exit(1);
});
