import path from "node:path";
import { COMPOSE_FILE, EVIDENCE_DIR, getNowIso, readJsonIfExists, runCommand, writeJson } from "./local_docker_common.mjs";

const readiness = await readJsonIfExists(path.join(EVIDENCE_DIR, "docker-daemon-readiness.json"));
let result = { code: 1, stdout: "", stderr: "Skipped because Docker daemon is not ready.", timedOut: false };
if (readiness?.ready) {
  result = await runCommand("docker", ["compose", "-f", COMPOSE_FILE, "up", "-d"], { timeoutMs: 120000 });
}
const payload = {
  generatedAt: getNowIso(),
  started: result.code === 0 && !result.timedOut,
  composeFile: COMPOSE_FILE,
  result,
};
await writeJson(path.join(EVIDENCE_DIR, "docker-compose-stack-status.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.started ? 0 : 1);
