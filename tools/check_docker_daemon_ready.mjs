import path from "node:path";
import { COMPOSE_FILE, EVIDENCE_DIR, getNowIso, runCommand, writeJson } from "./local_docker_common.mjs";

const composeVersion = await runCommand("docker", ["compose", "version"], { timeoutMs: 12000 });
const dockerInfo = await runCommand("docker", ["info"], { timeoutMs: 12000 });
const dockerPs = await runCommand("docker", ["ps", "--format", "table {{.Names}}\t{{.Status}}"], { timeoutMs: 12000 });
let composePs = {
  command: "docker",
  args: ["compose", "-f", COMPOSE_FILE, "ps"],
  code: null,
  stdout: "",
  stderr: "Skipped because docker daemon was not responsive enough for compose ps.",
  timedOut: false,
};
if (dockerInfo.code === 0 && !dockerInfo.timedOut && dockerPs.code === 0 && !dockerPs.timedOut) {
  composePs = await runCommand("docker", ["compose", "-f", COMPOSE_FILE, "ps"], { timeoutMs: 12000 });
}

const ready =
  dockerInfo.code === 0 &&
  !dockerInfo.timedOut &&
  dockerPs.code === 0 &&
  !dockerPs.timedOut &&
  composeVersion.code === 0 &&
  !composeVersion.timedOut &&
  composePs.code === 0 &&
  !composePs.timedOut;

const payload = {
  generatedAt: getNowIso(),
  ready,
  checks: { dockerInfo, dockerPs, composeVersion, composePs },
  summary: ready ? "Docker daemon and compose interface are ready." : "Docker daemon or compose interface is not fully responsive.",
};

await writeJson(path.join(EVIDENCE_DIR, "docker-daemon-readiness.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(ready ? 0 : 1);
