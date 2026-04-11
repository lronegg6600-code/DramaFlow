import { writeFileSync } from "node:fs";
import { spawnSync } from "node:child_process";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const output = path.join(root, "release-evidence", "docker-wsl-control-path-diagnosis.json");

function run(command, timeout = 40000) {
  const result = spawnSync("powershell.exe", ["-Command", command], {
    cwd: root,
    encoding: "utf8",
    timeout,
  });
  return {
    status: result.error?.code === "ETIMEDOUT" ? "timed_out" : result.status === 0 ? "ok" : "failed",
    exitCode: result.status,
    signal: result.signal,
    stdout: result.stdout?.slice(-4000) ?? "",
    stderr: result.stderr?.slice(-4000) ?? "",
  };
}

const payload = {
  generatedAt: new Date().toISOString(),
  dockerInfo: run("docker info"),
  dockerPs: run("docker ps --format \"table {{.ID}}\\t{{.Image}}\\t{{.Names}}\\t{{.Ports}}\\t{{.Status}}\""),
  composePs: run("docker compose -f 'Z:\\Projects\\DramaFlow\\backend\\deployments\\docker-compose\\docker-compose.yml' ps --format json"),
  composeLogs: run("docker compose -f 'Z:\\Projects\\DramaFlow\\backend\\deployments\\docker-compose\\docker-compose.yml' logs --tail=20 entitlement-service"),
  wslStatus: run("wsl.exe --status", 10000),
  wslDesktopExec: run("wsl.exe -d docker-desktop sh -lc \"ps -ef | head\"", 30000),
  lxssManager: run("sc.exe query LxssManager", 10000),
  dockerService: run("sc.exe query com.docker.service", 10000),
};

payload.interpretation = "Docker Desktop service can be running while docker CLI, compose control plane, and docker-desktop WSL exec remain unavailable. This is a control-path failure, not evidence of successful runtime cutover.";

writeFileSync(output, JSON.stringify(payload, null, 2));
console.log(JSON.stringify(payload, null, 2));
