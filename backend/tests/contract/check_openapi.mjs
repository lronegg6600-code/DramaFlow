import { readFileSync, existsSync } from "node:fs";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "..", "..");
const indexPath = resolve(root, "api", "openapi", "index.yaml");

const requiredSpecs = {
  "admin-service": ["/v1/admin/auth/login", "resource.not_found"],
  "billing-service": ["/v1/billing/google-play/purchases:sync", "request.invalid"],
  "entitlement-service": ["/v1/entitlements/grants", "auth.forbidden"],
  "playback-service": ["/v1/playback/sessions", "playback.entitlement_required"]
};

const indexText = readFileSync(indexPath, "utf8");

for (const [service, markers] of Object.entries(requiredSpecs)) {
  const specPath = resolve(root, "services", service, "openapi", "openapi.yaml");

  if (!indexText.includes(service)) {
    console.error(`[contract] missing service in index: ${service}`);
    process.exit(1);
  }
  if (!existsSync(specPath)) {
    console.error(`[contract] missing spec file: ${specPath}`);
    process.exit(1);
  }

  const specText = readFileSync(specPath, "utf8");
  if (!specText.includes("openapi:") || !specText.includes("paths:")) {
    console.error(`[contract] invalid spec header: ${service}`);
    process.exit(1);
  }

  for (const marker of markers) {
    if (!specText.includes(marker)) {
      console.error(`[contract] missing contract marker '${marker}' in ${service}`);
      process.exit(1);
    }
  }
}

console.log("[contract] openapi checks passed");
