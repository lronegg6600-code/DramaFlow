import { runPowerShell, writeJson, stamp } from './phase31_engine_common.mjs';
const result = await runPowerShell('sc.exe query com.docker.service');
await writeJson('docker-windows-service-inspection.json', stamp({ result, serviceRunning: /STATE\s+:\s+4\s+RUNNING/.test(result.stdout || '') }));
