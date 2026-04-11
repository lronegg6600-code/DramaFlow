import { runPowerShell, writeJson, stamp } from './phase31_engine_common.mjs';
const cmd = "Get-Process | Where-Object { $_.ProcessName -in 'Docker Desktop','com.docker.backend','dockerd' } | Select-Object ProcessName,Id,Path | ConvertTo-Json -Depth 4";
const result = await runPowerShell(cmd);
let processes = [];
if (result.stdout?.trim()) {
  try { processes = JSON.parse(result.stdout); } catch {}
}
await writeJson('docker-desktop-process-inspection.json', stamp({ processes, raw: result, interpretation: processes.length > 0 ? 'Docker backend processes are present.' : 'No Docker Desktop backend process is currently visible.' }));
