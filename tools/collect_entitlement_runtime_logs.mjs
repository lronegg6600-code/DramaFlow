import { writeFileSync } from "node:fs";
import { spawnSync } from "node:child_process";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const composeFile = path.join(root, "backend", "deployments", "docker-compose", "docker-compose.yml");
const output = path.join(root, "release-evidence", "entitlement-runtime-log-summary.json");

const composeLogs = spawnSync("powershell.exe", ["-Command", `docker compose -f "${composeFile}" logs --tail=80 entitlement-service`], {
  cwd: root,
  encoding: "utf8",
  timeout: 30000,
});
const wslShell = spawnSync("powershell.exe", ["-Command", `wsl.exe -d docker-desktop sh -lc "ps -ef | grep entitlement-service | grep -v grep"`], {
  cwd: root,
  encoding: "utf8",
  timeout: 30000,
});

const payload = {
  generatedAt: new Date().toISOString(),
  composeLogs: {
    status: composeLogs.error?.code === "ETIMEDOUT" ? "timed_out" : composeLogs.status === 0 ? "captured" : "failed",
    stdout: composeLogs.stdout?.slice(-4000) ?? "",
    stderr: composeLogs.stderr?.slice(-4000) ?? "",
  },
  wslShell: {
    status: wslShell.status === 0 ? "captured" : "failed",
    stdout: wslShell.stdout?.slice(-4000) ?? "",
    stderr: wslShell.stderr?.slice(-4000) ?? "",
  },
  interpretation: "Docker/WSL management path remains degraded if logs cannot be collected or WSL shell creation fails.",
};

writeFileSync(output, JSON.stringify(payload, null, 2));
console.log(JSON.stringify(payload, null, 2));
