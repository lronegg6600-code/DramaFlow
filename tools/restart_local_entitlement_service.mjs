import { writeFileSync } from "node:fs";
import { spawnSync } from "node:child_process";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const composeFile = path.join(root, "backend", "deployments", "docker-compose", "docker-compose.yml");
const output = path.join(root, "release-evidence", "entitlement-runtime-restart-result.json");

const startedAt = new Date().toISOString();
const command = `docker compose -f "${composeFile}" up --build -d entitlement-service`;
const result = spawnSync("powershell.exe", ["-Command", command], {
  cwd: root,
  encoding: "utf8",
  timeout: 15 * 60 * 1000,
});

const payload = {
  generatedAt: new Date().toISOString(),
  startedAt,
  command,
  status:
    result.error?.code === "ETIMEDOUT"
      ? "timed_out"
      : result.status === 0
        ? "succeeded"
        : "failed",
  exitCode: result.status,
  signal: result.signal,
  timedOut: result.error?.code === "ETIMEDOUT",
  stdout: result.stdout?.slice(-4000) ?? "",
  stderr: result.stderr?.slice(-4000) ?? "",
  interpretation:
    result.error?.code === "ETIMEDOUT"
      ? "docker compose control path did not complete within the timeout; runtime replacement is not proven"
      : result.status === 0
        ? "compose reported success; follow-up runtime probe is still required"
        : "compose returned a failure status; runtime replacement is not proven",
};

writeFileSync(output, JSON.stringify(payload, null, 2));
console.log(JSON.stringify(payload, null, 2));
process.exit(result.status ?? (result.error ? 1 : 0));
