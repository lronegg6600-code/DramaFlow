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
  const outFile = path.join(evidenceDir, "cluster-access.json");
  const targetEnv = process.env.DRAMAFLOW_TARGET_ENV ?? "staging";
  const namespace = process.env.DRAMAFLOW_KUBE_NAMESPACE ?? null;

  const kubectlPath = run("where.exe", ["kubectl"], root);
  const currentContext = kubectlPath ? run("kubectl", ["config", "current-context"], root) : null;
  const contexts = kubectlPath ? run("kubectl", ["config", "get-contexts", "-o=name"], root) : null;

  const blockers = [];
  if (!kubectlPath) blockers.push("kubectl is not available");
  if (kubectlPath && !currentContext) blockers.push("kubectl current-context is not set");
  if (kubectlPath && !(contexts && contexts.length > 0)) blockers.push("no kubernetes contexts are configured");
  if (!namespace) blockers.push("target namespace is not provided");
  if (!process.env.DRAMAFLOW_KUBECONFIG_SOURCE) blockers.push("kubeconfig source is not declared");
  if (!process.env.DRAMAFLOW_CLUSTER_RBAC_ROLE) blockers.push("cluster RBAC role is not declared");

  const result = {
    category: "cluster_access",
    status: blockers.length === 0 ? "pass" : "blocker",
    generatedAt: new Date().toISOString(),
    targetEnv,
    kubectlPath,
    currentContext,
    contexts: contexts ? contexts.split(/\r?\n/).filter(Boolean) : [],
    namespace,
    kubeconfigSource: process.env.DRAMAFLOW_KUBECONFIG_SOURCE ?? null,
    clusterRbacRole: process.env.DRAMAFLOW_CLUSTER_RBAC_ROLE ?? null,
    blockers
  };

  writeResult(outFile, result);

  if (blockers.length > 0) {
    console.error("[verify-cluster-access] blocker");
    for (const blocker of blockers) console.error(`- ${blocker}`);
    process.exit(1);
  }

  console.log("[verify-cluster-access] pass");
}

main().catch((error) => {
  console.error(`[verify-cluster-access] fail: ${error.message}`);
  process.exit(1);
});
