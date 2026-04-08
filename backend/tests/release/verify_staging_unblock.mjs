import path from "node:path";
import { evidenceDir, readJson, writeJson } from "./_real_input_helpers.mjs";

async function main() {
  const dir = evidenceDir();
  const categories = [
    ["repository_identity", "real-repo-identity.json"],
    ["artifact_identity", "real-artifact-identity.json"],
    ["cluster_access", "real-cluster-access.json"],
    ["github_release_control", "real-github-environments.json"],
    ["secrets_config", "real-secret-readiness.json"]
  ];

  const checks = [];
  const blockers = [];

  for (const [category, file] of categories) {
    const payload = readJson(path.join(dir, file));
    const passed = payload.summary?.status === "verified";
    checks.push({ category, file, passed, blockerCount: payload.summary?.blockerCount ?? 0 });
    if (!passed) {
      blockers.push({
        category,
        blockerCount: payload.summary?.blockerCount ?? 0,
        items: payload.items.filter((item) => item.state !== "verified").map((item) => ({
          name: item.name,
          state: item.state,
          details: item.details,
          owner: item.owner
        }))
      });
    }
  }

  const deployToolingPath = path.join(dir, "deploy-tooling.json");
  let deployTooling = null;
  try {
    deployTooling = readJson(deployToolingPath);
  } catch {
    deployTooling = null;
  }
  if (deployTooling?.status !== "pass") {
    blockers.push({
      category: "deploy_tooling",
      blockerCount: 1,
      items: [{ name: "deploy tooling", state: "invalid", details: "helm missing or tooling incomplete", owner: "platform / ops" }]
    });
  }

  const ready = blockers.length === 0;
  const summary = {
    generatedAt: new Date().toISOString(),
    readyForRealStagingExecution: ready,
    checks,
    blockerCategoriesRemaining: blockers.length,
    blockers
  };

  writeJson(path.join(dir, "platform-input-intake.json"), summary);
  writeJson(path.join(dir, "staging-unblock-summary.json"), summary);
  writeJson(path.join(dir, "staging-rehearsal-real.json"), {
    generatedAt: new Date().toISOString(),
    status: ready ? "ready_to_execute" : "not_executed",
    reason: ready ? "all staging prerequisites verified" : "remaining blockers prevent real staging rehearsal"
  });

  if (!ready) {
    console.error("[verify-staging-unblock] blocker");
    for (const blocker of blockers) {
      console.error(`- ${blocker.category}: ${blocker.blockerCount} blocker(s)`);
    }
    process.exit(1);
  }

  console.log("[verify-staging-unblock] pass");
}

main().catch((error) => {
  console.error(`[verify-staging-unblock] fail: ${error.message}`);
  process.exit(1);
});
