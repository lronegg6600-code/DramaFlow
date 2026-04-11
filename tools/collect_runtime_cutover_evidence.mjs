import { writeFileSync, readFileSync, existsSync } from "node:fs";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const output = path.join(root, "release-evidence", "entitlement-runtime-cutover-evidence.json");

function readJson(name) {
  const file = path.join(root, "release-evidence", name);
  return existsSync(file) ? JSON.parse(readFileSync(file, "utf8")) : null;
}

const payload = {
  generatedAt: new Date().toISOString(),
  diagnosis: readJson("docker-wsl-control-path-diagnosis.json"),
  instance: readJson("entitlement-live-instance-identification.json"),
  provider: readJson("entitlement-port-8086-provider.json"),
  composeCutover: readJson("entitlement-compose-cutover-result.json"),
  alternativeCutover: readJson("entitlement-alternative-cutover-result.json"),
  probe: readJson("entitlement-live-contract-reprobe.json"),
};

writeFileSync(output, JSON.stringify(payload, null, 2));
console.log(JSON.stringify(payload, null, 2));
