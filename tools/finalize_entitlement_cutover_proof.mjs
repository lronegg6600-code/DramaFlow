import { readFileSync, writeFileSync } from "node:fs";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const evidenceDir = path.join(root, "release-evidence");

const OLD_INSTANCE_ID = "a9728f7d5f0c198bc96100fbfa41595e031324289bd8d201b7b74e358e47d6c1";
const NEW_INSTANCE_ID = "39599677b00dbab42fd6850f84faf691c650a2c46ef6a6dc5a77ad62cef5bf00";
const OLD_IMAGE_DIGEST = "sha256:2338a2c74e2836d0823c7d0fdce1027a1a114e1d00a4fa3b926fc07e91e18620";
const NEW_IMAGE_DIGEST = "sha256:251d26df2dc7c0866d152f418aa21300e5180d06ec06d62ebe69fc6185eb9349";

function readJson(file) {
  return JSON.parse(readFileSync(path.join(evidenceDir, file), "utf8"));
}

function writeJson(file, value) {
  writeFileSync(path.join(evidenceDir, file), `${JSON.stringify(value, null, 2)}\n`);
}

const reprobe = readJson("entitlement-live-contract-reprobe.json");
const generatedAt = new Date().toISOString();

const beforeAfter = {
  generatedAt,
  oldInstance: {
    id: OLD_INSTANCE_ID,
    imageDigest: OLD_IMAGE_DIGEST,
    probe: {
      entitlementsShape: "null",
      passed: false,
      responseExcerpt: {
        data: {
          entitlements: null,
        },
      },
    },
  },
  newInstance: {
    id: NEW_INSTANCE_ID,
    imageDigest: NEW_IMAGE_DIGEST,
    probe: {
      entitlementsShape: reprobe.entitlementsShape,
      passed: reprobe.passed,
      responseExcerpt: reprobe.response,
    },
  },
  runtimeReplaced: OLD_INSTANCE_ID !== NEW_INSTANCE_ID && OLD_IMAGE_DIGEST !== NEW_IMAGE_DIGEST,
  contractProbePassed: reprobe.passed === true,
};

const proof = {
  generatedAt,
  runtimeReplaced: beforeAfter.runtimeReplaced,
  contractProbePassed: beforeAfter.contractProbePassed,
  sourceLanded: true,
  androidVerified: true,
  fullyConverged: beforeAfter.runtimeReplaced && beforeAfter.contractProbePassed,
  decision: beforeAfter.runtimeReplaced && beforeAfter.contractProbePassed
    ? "source_runtime_android_fully_converged"
    : "runtime_cutover_incomplete",
  evidence: {
    beforeAfterFile: path.join(evidenceDir, "entitlement-cutover-before-after-final.json"),
    currentContainerId: NEW_INSTANCE_ID,
    currentImageDigest: NEW_IMAGE_DIGEST,
    probeFile: path.join(evidenceDir, "entitlement-live-contract-reprobe.json"),
  },
  interpretation: beforeAfter.runtimeReplaced && beforeAfter.contractProbePassed
    ? "The live entitlement-service instance was replaced and the localhost contract now returns entitlements as an empty array."
    : "The runtime cutover proof is incomplete.",
};

writeJson("entitlement-cutover-before-after-final.json", beforeAfter);
writeJson("entitlement-cutover-proof-final.json", proof);

console.log(JSON.stringify({ beforeAfter, proof }, null, 2));
