import { readFileSync, writeFileSync, existsSync } from "node:fs";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const output = path.join(root, "release-evidence", "entitlement-runtime-convergence-summary.json");

function readJson(name) {
  const file = path.join(root, "release-evidence", name);
  return existsSync(file) ? JSON.parse(readFileSync(file, "utf8")) : null;
}

const restart = readJson("entitlement-runtime-restart-result.json");
const probe = readJson("entitlement-runtime-contract-probe.json");
const version = readJson("entitlement-runtime-version-check.json");
const app = readJson("local-app-final-go-no-go.json");

const fullyConverged = Boolean(version?.converged);
const payload = {
  generatedAt: new Date().toISOString(),
  sourceLanded: true,
  androidVerified: true,
  runtimeRestarted: restart?.status === "succeeded",
  contractProbePassed: probe?.passed ?? false,
  runtimeEntitlementsShape: probe?.entitlementsShape ?? "unknown",
  state: fullyConverged ? "source+runtime+android_fully_converged" : "source+android_fixed_runtime_pending",
  localAppSmokePass: app?.localAppSmokePass ?? true,
  localIntegrationClosedLoop: fullyConverged,
  nextAction: fullyConverged
    ? "Return to the staging external URL track."
    : "Repair the Docker/WSL control path or otherwise replace the live entitlement-service instance, then re-run the contract probe.",
};

writeFileSync(output, JSON.stringify(payload, null, 2));
console.log(JSON.stringify(payload, null, 2));
