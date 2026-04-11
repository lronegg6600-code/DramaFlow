import { writeFileSync } from "node:fs";
import { spawnSync } from "node:child_process";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const output = path.join(root, "release-evidence", "entitlement-live-instance-identification.json");

function run(command, timeout = 15000) {
  const result = spawnSync("powershell.exe", ["-Command", command], {
    cwd: root,
    encoding: "utf8",
    timeout,
  });
  return result.stdout?.trim() ?? "";
}

const listeners = run("Get-NetTCPConnection -LocalPort 8086 -State Listen -ErrorAction SilentlyContinue | Select-Object LocalAddress,LocalPort,OwningProcess | ConvertTo-Json -Compress");
const processes = run("Get-Process -Id 21628,28780 -ErrorAction SilentlyContinue | Select-Object Id,ProcessName,Path | ConvertTo-Json -Depth 3");

const payload = {
  generatedAt: new Date().toISOString(),
  listeners8086: listeners || null,
  knownProviderProcesses: processes || null,
  interpretation: listeners
    ? "8086 still has a live provider and must be mapped to Docker/WSL forwarding."
    : "8086 currently has no live provider after the Docker Desktop restart attempt.",
};

writeFileSync(output, JSON.stringify(payload, null, 2));
console.log(JSON.stringify(payload, null, 2));
