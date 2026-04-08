import path from "node:path";
import { evidenceDir, item, printAndExit, summarize, writeJson } from "./_real_input_helpers.mjs";

async function main() {
  const outFile = path.join(evidenceDir(), "real-secret-readiness.json");
  const required = [
    ["DRAMAFLOW_POSTGRES_DSN", "platform/security"],
    ["DRAMAFLOW_REDIS_ADDR", "platform/security"],
    ["DRAMAFLOW_JWT_SECRET", "platform/security"],
    ["ADMIN_SESSION_SECRET", "platform/security"],
    ["ADMIN_BOOTSTRAP_PASSWORD", "release manager / security"],
    ["GOOGLE_PLAY_SERVICE_ACCOUNT_JSON", "platform/security"],
    ["BILLING_RTDN_PUSH_SECRET", "platform/security"],
    ["DRAMAFLOW_REGISTRY", "platform / registry admin"],
    ["DRAMAFLOW_REGISTRY_USERNAME", "platform / registry admin"],
    ["DRAMAFLOW_REGISTRY_PASSWORD", "platform / registry admin"],
    ["DRAMAFLOW_CLOUD_SERVICE_ACCOUNT", "platform/security"],
    ["DRAMAFLOW_DEPLOY_TOKEN", "repo admin / platform"]
  ];

  const items = required.map(([name, owner]) =>
    item(name, process.env[name] ? "verified" : "missing", process.env[name] ? "present in environment" : "not provided", owner)
  );

  const payload = {
    category: "secrets_config",
    generatedAt: new Date().toISOString(),
    items,
    summary: summarize(items)
  };

  writeJson(outFile, payload);
  printAndExit("verify-real-secret-readiness", payload);
}

main().catch((error) => {
  console.error(`[verify-real-secret-readiness] fail: ${error.message}`);
  process.exit(1);
});
