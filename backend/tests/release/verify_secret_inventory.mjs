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
  const outFile = path.join(evidenceDir, "secret-inventory.json");
  const targetEnv = process.env.DRAMAFLOW_TARGET_ENV ?? "staging";

  const required = [
    "DRAMAFLOW_POSTGRES_DSN",
    "DRAMAFLOW_REDIS_ADDR",
    "DRAMAFLOW_JWT_SECRET",
    "ADMIN_SESSION_SECRET",
    "ADMIN_BOOTSTRAP_PASSWORD",
    "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON",
    "BILLING_RTDN_PUSH_SECRET",
    "DRAMAFLOW_REGISTRY",
    "DRAMAFLOW_REGISTRY_USERNAME",
    "DRAMAFLOW_REGISTRY_PASSWORD",
    "DRAMAFLOW_CLOUD_SERVICE_ACCOUNT",
    "DRAMAFLOW_DEPLOY_TOKEN"
  ];

  const missing = required.filter((name) => !process.env[name]);
  const blockers = missing.map((name) => `missing required secret/config: ${name}`);

  const result = {
    category: "secrets_config",
    status: blockers.length === 0 ? "pass" : "blocker",
    generatedAt: new Date().toISOString(),
    targetEnv,
    required,
    missing,
    blockers
  };

  writeResult(outFile, result);

  if (blockers.length > 0) {
    console.error("[verify-secret-inventory] blocker");
    for (const blocker of blockers) console.error(`- ${blocker}`);
    process.exit(1);
  }

  console.log("[verify-secret-inventory] pass");
}

main().catch((error) => {
  console.error(`[verify-secret-inventory] fail: ${error.message}`);
  process.exit(1);
});
