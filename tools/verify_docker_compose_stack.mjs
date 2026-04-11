import fs from "node:fs/promises";
import path from "node:path";
import { COMPOSE_FILE, EVIDENCE_DIR, getNowIso, runCommand, writeJson } from "./local_docker_common.mjs";

const dockerReadinessPath = path.join(EVIDENCE_DIR, "docker-daemon-readiness.json");
let dockerReadiness = null;
try {
  dockerReadiness = JSON.parse(await fs.readFile(dockerReadinessPath, "utf8"));
} catch {
  dockerReadiness = null;
}

const dockerResponsive = Boolean(dockerReadiness?.checks?.dockerInfo && dockerReadiness?.checks?.dockerPs)
  && !dockerReadiness.checks.dockerInfo.timedOut
  && !dockerReadiness.checks.dockerPs.timedOut
  && dockerReadiness.checks.dockerInfo.code === 0
  && dockerReadiness.checks.dockerPs.code === 0;

const composePs = dockerResponsive
  ? await runCommand("docker", ["compose", "-f", COMPOSE_FILE, "ps", "--format", "json"], { timeoutMs: 15000 })
  : {
      command: "docker",
      args: ["compose", "-f", COMPOSE_FILE, "ps", "--format", "json"],
      code: null,
      stdout: "",
      stderr: "Skipped because docker daemon was not responsive enough for compose ps.",
      timedOut: false,
    };

const ready = composePs.code === 0 && !composePs.timedOut;
const payload = {
  generatedAt: getNowIso(),
  composeFile: path.relative(process.cwd(), COMPOSE_FILE),
  dockerResponsive,
  ready,
  composePs,
  summary: ready
    ? "Compose stack status is reachable through Docker CLI."
    : dockerResponsive
      ? "Docker CLI could not return compose stack status in time."
      : "Compose stack status skipped because docker daemon is not responsive via CLI.",
};
await writeJson(path.join(EVIDENCE_DIR, "docker-compose-stack-status.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(ready ? 0 : 1);
