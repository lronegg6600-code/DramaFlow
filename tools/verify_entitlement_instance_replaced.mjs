import { writeFileSync } from "node:fs";
import { spawnSync } from "node:child_process";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const output = path.join(root, "release-evidence", "entitlement-instance-replaced-check.json");

const listener = spawnSync("powershell.exe", ["-Command", "Get-NetTCPConnection -LocalPort 8086 -State Listen -ErrorAction SilentlyContinue | Select-Object LocalAddress,LocalPort,OwningProcess | ConvertTo-Json -Compress"], {
  cwd: root,
  encoding: "utf8",
  timeout: 10000,
});

const payload = {
  generatedAt: new Date().toISOString(),
  listener8086: listener.stdout?.trim() || null,
  replaced: false,
  interpretation: listener.stdout?.trim()
    ? "Provider exists but replacement is not proven without a passing contract probe."
    : "No listener on 8086, so replacement is not proven and runtime is currently unavailable.",
};

writeFileSync(output, JSON.stringify(payload, null, 2));
console.log(JSON.stringify(payload, null, 2));
