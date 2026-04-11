import { writeFileSync } from "node:fs";
import { spawnSync } from "node:child_process";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const output = path.join(root, "release-evidence", "entitlement-port-8086-provider.json");

function run(command, timeout = 15000) {
  const result = spawnSync("powershell.exe", ["-Command", command], {
    cwd: root,
    encoding: "utf8",
    timeout,
  });
  return {
    status: result.error?.code === "ETIMEDOUT" ? "timed_out" : result.status === 0 ? "ok" : "failed",
    stdout: result.stdout?.trim() ?? "",
    stderr: result.stderr?.trim() ?? "",
  };
}

const netstat = run("netstat -ano | Select-String ':8086'");
const tcp = run("Get-NetTCPConnection -LocalPort 8086 -State Listen -ErrorAction SilentlyContinue | Select-Object LocalAddress,LocalPort,OwningProcess | ConvertTo-Json -Compress");

const payload = {
  generatedAt: new Date().toISOString(),
  netstat,
  tcp,
  providerAvailable: Boolean(tcp.stdout),
  interpretation: tcp.stdout
    ? "A Windows-side listener still fronts 8086."
    : "No 8086 listener is currently available; runtime cutover cannot be verified.",
};

writeFileSync(output, JSON.stringify(payload, null, 2));
console.log(JSON.stringify(payload, null, 2));
