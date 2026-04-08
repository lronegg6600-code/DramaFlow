import path from "node:path";

import {
  intakeRoot,
  listCandidateFiles,
  parseLooseConfig,
  statusForValue,
  validationPayload,
  writeValidationFiles,
  printValidationResult
} from "./_intake_validation_helpers.mjs";

const category = "secret_inventory_input_package";
const dir = path.join(intakeRoot(), "received", "secrets");
const files = listCandidateFiles(dir);
const sourceFile = files[0] ?? null;
const data = sourceFile ? parseLooseConfig(sourceFile) : {};

const fields = [
  ["SC-01", "DRAMAFLOW_POSTGRES_DSN", "postgres_dsn_secret_ref"],
  ["SC-02", "DRAMAFLOW_REDIS_ADDR", "redis_addr_secret_ref"],
  ["SC-03", "DRAMAFLOW_JWT_SECRET", "jwt_secret_ref"],
  ["SC-04", "ADMIN_SESSION_SECRET", "admin_session_secret_ref"],
  ["SC-05", "ADMIN_BOOTSTRAP_PASSWORD", "admin_bootstrap_password_ref"],
  ["SC-06", "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON", "google_play_service_account_secret_ref"],
  ["SC-07", "BILLING_RTDN_PUSH_SECRET", "billing_rtdn_push_secret_ref"],
  ["SC-08", "DRAMAFLOW_REGISTRY", "registry_host"],
  ["SC-09", "DRAMAFLOW_REGISTRY_USERNAME", "registry_username_secret_ref"],
  ["SC-10", "DRAMAFLOW_REGISTRY_PASSWORD", "registry_password_secret_ref"],
  ["SC-11", "DRAMAFLOW_CLOUD_SERVICE_ACCOUNT", "cloud_service_account_secret_ref"],
  ["SC-12", "DRAMAFLOW_DEPLOY_TOKEN", "deploy_token_secret_ref"]
];

const items = fields.map(([blockerId, title, field]) => ({
  blocker_id: blockerId,
  blocker_title: title,
  current_status: sourceFile ? statusForValue(data[field]) : "not_received",
  field,
  notes: sourceFile ? "需要 secret ref / vault path / secret name，而不是明文" : "未收到 secret inventory 输入包"
}));

const payload = validationPayload(category, sourceFile, items);
writeValidationFiles(category, payload);
printValidationResult(category, payload);
