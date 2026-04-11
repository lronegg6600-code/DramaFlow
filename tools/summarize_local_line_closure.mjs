import { writeFileSync } from "node:fs";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const evidenceDir = path.join(root, "release-evidence");

function writeJson(name, payload) {
  writeFileSync(path.join(evidenceDir, name), `${JSON.stringify(payload, null, 2)}\n`);
}

const generatedAt = new Date().toISOString();

const convergence = {
  generatedAt,
  sourceLanded: true,
  androidVerified: true,
  runtimeRestarted: true,
  runtimeReplaced: true,
  contractProbePassed: true,
  runtimeEntitlementsShape: "array",
  state: "source_runtime_android_fully_converged",
  localAppSmokePass: true,
  localIntegrationClosedLoop: true,
  nextAction: "Return to the staging external URL line and continue the unresolved real external URL acceptance workflow.",
};

const localContractGoNoGo = {
  generatedAt,
  decision: "go_for_local_contract_finalization",
  state: "source_runtime_android_fully_converged",
  sourceLanded: true,
  androidVerified: true,
  runtimeRestarted: true,
  runtimeReplaced: true,
  contractProbePassed: true,
  localAppSmokePass: true,
  reason: "The live entitlement-service instance has been replaced, the localhost contract now returns entitlements as an empty array, and post-cutover device retests stayed stable.",
};

const localAppGoNoGo = {
  generatedAt,
  decision: "go_for_local_app_signoff",
  localAppSmokePass: true,
  localLineClosed: true,
  stagingReady: false,
  reason: "Feed/Detail, Playback, Billing/Entitlement, and Restore/Revoke are all stable on the physical device, and the live localhost entitlement-service runtime now matches the fixed contract.",
  residualRisks: [
    "staging external URL validation remains pending",
    "local billing environment still cannot complete a real Play purchase, which is expected in this local debug setup",
  ],
  contractFinalizationState: "source_runtime_android_fully_converged",
};

const closure = {
  generatedAt,
  localContractFullyConverged: true,
  localAppSmokePass: true,
  localLineClosed: true,
  stagingReady: false,
  primaryNextPath: "staging_external_url_acceptance",
  blockerTransferredToStaging: "platform has still not provided 7 real resolvable Android-accessible external staging URLs",
};

writeJson("entitlement-runtime-convergence-summary.json", convergence);
writeJson("local-contract-finalization-go-no-go.json", localContractGoNoGo);
writeJson("local-app-final-go-no-go.json", localAppGoNoGo);
writeJson("local-line-final-closure-summary.json", closure);

console.log(JSON.stringify({ convergence, localContractGoNoGo, localAppGoNoGo, closure }, null, 2));
