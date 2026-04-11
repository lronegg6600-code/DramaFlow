import { runPowerShell, writeJson, stamp } from './phase31_engine_common.mjs';
const result = await runPowerShell("Test-Path \\\\.\\pipe\\dockerDesktopLinuxEngine | ConvertTo-Json");
const exists = /true/i.test(result.stdout || '');
await writeJson('docker-linux-engine-pipe-inspection.json', stamp({ exists, result, interpretation: exists ? 'Linux engine pipe is present.' : 'Linux engine pipe is missing.' }));
