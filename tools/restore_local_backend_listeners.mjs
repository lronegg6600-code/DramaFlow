import { runPowerShell, writeJson, stamp } from './phase31_engine_common.mjs';
const up = await runPowerShell('docker compose -f backend\\deployments\\docker-compose\\docker-compose.yml up -d', 300000);
const ps = await runPowerShell('docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"');
await writeJson('local-backend-listener-restore-result.json', stamp({ composeUp: up, dockerPs: ps, interpretation: up.status === 'ok' ? 'Compose stack restore command completed.' : 'Compose stack restore command failed.' }));
