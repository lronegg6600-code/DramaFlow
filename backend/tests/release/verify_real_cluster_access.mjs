import path from "node:path";
import { evidenceDir, item, printAndExit, rootDir, run, summarize, writeJson } from "./_real_input_helpers.mjs";

async function main() {
  const root = rootDir();
  const outFile = path.join(evidenceDir(), "real-cluster-access.json");
  const targetEnv = process.env.DRAMAFLOW_TARGET_ENV ?? "staging";
  const namespace = process.env.DRAMAFLOW_KUBE_NAMESPACE ?? null;
  const kubeconfigSource = process.env.DRAMAFLOW_KUBECONFIG_SOURCE ?? null;
  const rbacRole = process.env.DRAMAFLOW_CLUSTER_RBAC_ROLE ?? null;

  const kubectlPath = run("where.exe", ["kubectl"], root);
  const currentContext = kubectlPath ? run("kubectl", ["config", "current-context"], root) : null;
  const contextsRaw = kubectlPath ? run("kubectl", ["config", "get-contexts", "-o=name"], root) : null;
  const contexts = contextsRaw ? contextsRaw.split(/\r?\n/).filter(Boolean) : [];
  const canReadDeployments = currentContext && namespace ? run("kubectl", ["auth", "can-i", "get", "deployments", "-n", namespace], root) : null;
  const canPatchDeployments = currentContext && namespace ? run("kubectl", ["auth", "can-i", "patch", "deployments", "-n", namespace], root) : null;

  const items = [
    item("kubectl binary", kubectlPath ? "verified" : "missing", kubectlPath ?? "kubectl not found", "platform / ops"),
    item("current context", currentContext ? "verified" : "missing", currentContext ?? "kubectl current-context unavailable", "platform / ops"),
    item("target context list", contexts.length > 0 ? "verified" : "missing", contexts.length > 0 ? contexts.join(", ") : "no contexts configured", "platform / ops"),
    item("namespace", namespace ? "verified" : "missing", namespace ?? "DRAMAFLOW_KUBE_NAMESPACE not set", "platform / ops"),
    item("kubeconfig source", kubeconfigSource ? "verified" : "missing", kubeconfigSource ?? "DRAMAFLOW_KUBECONFIG_SOURCE not set", "platform / ops"),
    item("RBAC role", rbacRole ? "verified" : "missing", rbacRole ?? "DRAMAFLOW_CLUSTER_RBAC_ROLE not set", "platform / ops"),
    item("deployment read access", canReadDeployments === "yes" ? "verified" : currentContext ? "invalid" : "missing", canReadDeployments ?? "kubectl auth can-i get deployments unavailable", "platform / ops"),
    item("deployment patch access", canPatchDeployments === "yes" ? "verified" : currentContext ? "invalid" : "missing", canPatchDeployments ?? "kubectl auth can-i patch deployments unavailable", "platform / ops")
  ];

  const payload = {
    category: "cluster_access",
    generatedAt: new Date().toISOString(),
    targetEnv,
    items,
    summary: summarize(items),
    values: { kubectlPath, currentContext, contexts, namespace, kubeconfigSource, rbacRole, canReadDeployments, canPatchDeployments }
  };

  writeJson(outFile, payload);
  printAndExit("verify-real-cluster-access", payload);
}

main().catch((error) => {
  console.error(`[verify-real-cluster-access] fail: ${error.message}`);
  process.exit(1);
});
