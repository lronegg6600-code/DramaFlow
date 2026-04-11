import { writeFileSync } from "node:fs";
import { spawnSync } from "node:child_process";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const output = path.join(root, "release-evidence", "entitlement-compose-cutover-result.json");
const command = "docker compose -f 'Z:\\Projects\\DramaFlow\\backend\\deployments\\docker-compose\\docker-compose.yml' up --build -d entitlement-service";

const result = spawnSync("powershell.exe", ["-Command", command], {
  cwd: root,
  encoding: "utf8",
  timeout: 5 * 60 * 1000,
});

const payload = {
  generatedAt: new Date().toISOString(),
  command,
  status: result.error?.code === "ETIMEDOUT" ? "timed_out" : result.status === 0 ? "succeeded" : "failed",
  exitCode: result.status,
  signal: result.signal,
  stdout: result.stdout?.slice(-4000) ?? "",
  stderr: result.stderr?.slice(-4000) ?? "",
  interpretation:
    result.error?.code === "ETIMEDOUT"
      ? "Compose cutover path remains blocked."
      : result.status === 0
        ? "Compose reported success; follow-up provider and contract checks still required."
        : "Compose cutover failed immediately.",
};

writeFileSync(output, JSON.stringify(payload, null, 2));
console.log(JSON.stringify(payload, null, 2));
process.exit(result.status ?? (result.error ? 1 : 0));
