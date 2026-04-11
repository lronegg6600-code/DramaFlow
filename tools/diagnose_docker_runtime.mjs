import path from "node:path";
import { EVIDENCE_DIR, getNowIso, runCommand, runPowershell, writeJson } from "./local_docker_common.mjs";

const serviceStatus = await runPowershell("sc.exe query com.docker.service", { timeoutMs: 10000 });
const processStatus = await runPowershell("Get-Process -Name \"Docker Desktop\",\"com.docker.backend\",\"wslservice\" -ErrorAction SilentlyContinue | Select-Object ProcessName,Id,CPU,StartTime | ConvertTo-Json -Depth 3", { timeoutMs: 10000 });
const pipeStatus = await runPowershell("@{ docker_engine = (Test-Path \\\\.\\pipe\\docker_engine); dockerDesktopLinuxEngine = (Test-Path \\\\.\\pipe\\dockerDesktopLinuxEngine) } | ConvertTo-Json", { timeoutMs: 10000 });
const wslStatus = await runPowershell("& 'C:\\Windows\\System32\\wsl.exe' --list --verbose", { timeoutMs: 10000 });
const dockerContext = await runCommand("docker", ["context", "inspect", "desktop-linux"], { timeoutMs: 10000 });
const dockerInfo = await runCommand("docker", ["info"], { timeoutMs: 12000 });

const payload = {
  generatedAt: getNowIso(),
  rootCause: dockerInfo.timedOut
    ? "Docker Desktop backend appears present, but Docker daemon handshake is not responding on the named pipe."
    : dockerInfo.code !== 0
      ? "Docker CLI returned an error while contacting the engine."
      : "Docker daemon responded normally.",
  serviceStatus,
  processStatus: processStatus.stdout || processStatus.stderr,
  pipeStatus: pipeStatus.stdout || pipeStatus.stderr,
  wslStatus: wslStatus.stdout || wslStatus.stderr,
  dockerContext,
  dockerInfo,
};

await writeJson(path.join(EVIDENCE_DIR, "docker-runtime-diagnosis.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(dockerInfo.code === 0 && !dockerInfo.timedOut ? 0 : 1);
