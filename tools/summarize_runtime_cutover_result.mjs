import { writeFileSync, readFileSync, existsSync } from "node:fs";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const output = path.join(root, "release-evidence", "entitlement-runtime-finalization-summary.json");

function readJson(name) {
  const file = path.join(root, "release-evidence", name);
  return existsSync(file) ? JSON.parse(readFileSync(file, "utf8")) : null;
}

const diagnosis = readJson("docker-wsl-control-path-diagnosis.json");
const compose = readJson("entitlement-compose-cutover-result.json");
const alternative = readJson("entitlement-alternative-cutover-result.json");
const replaced = readJson("entitlement-instance-replaced-check.json");
const reprobe = readJson("entitlement-live-contract-reprobe.json");

const fullyConverged = Boolean(reprobe?.passed);
const payload = {
  generatedAt: new Date().toISOString(),
  dockerWslControlFixed: false,
  providerConfirmed: Boolean(replaced?.listener8086),
  runtimeReplaced: fullyConverged,
  contractProbePassed: fullyConverged,
  state: fullyConverged ? "source+runtime+android_fully_converged" : "runtime_replacement_blocked",
  composePathStatus: compose?.status ?? "unknown",
  alternativePathStatus: alternative?.status ?? "unknown",
  reprobeStatus: reprobe?.status ?? (reprobe?.passed ? "pass" : "fail"),
  nextAction: fullyConverged
    ? "Return to staging external URL validation."
    : "Repair Docker/WSL control path first; no safe live instance cutover path is currently available.",
  diagnosisSummary: diagnosis?.interpretation ?? null,
};

writeFileSync(output, JSON.stringify(payload, null, 2));
console.log(JSON.stringify(payload, null, 2));
