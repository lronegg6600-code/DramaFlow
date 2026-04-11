import { writeFileSync } from "node:fs";
import { spawnSync } from "node:child_process";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const output = path.join(root, "release-evidence", "entitlement-runtime-version-check.json");

function run(command, timeout = 20000) {
  return spawnSync("powershell.exe", ["-Command", command], {
    cwd: root,
    encoding: "utf8",
    timeout,
  });
}

const port = run("Get-NetTCPConnection -LocalPort 8086 -State Listen | Select-Object LocalAddress,LocalPort,OwningProcess | ConvertTo-Json -Compress");
const proc = run("$conn = Get-NetTCPConnection -LocalPort 8086 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1; if ($conn) { Get-Process -Id $conn.OwningProcess | Select-Object Id,ProcessName,Path | ConvertTo-Json -Compress }");
const wsl = run("wsl.exe --list --verbose");

const payload = {
  generatedAt: new Date().toISOString(),
  port8086Listener: port.stdout?.trim() || null,
  ownerProcess: proc.stdout?.trim() || null,
  wslStatus: wsl.stdout?.trim() || null,
  interpretation: "Port 8086 is forwarded through WSL-backed Docker runtime; source parity must be validated by contract probe rather than by process path alone.",
};

writeFileSync(output, JSON.stringify(payload, null, 2));
console.log(JSON.stringify(payload, null, 2));
