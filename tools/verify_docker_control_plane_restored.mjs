import { runPowerShell, writeJson, stamp } from './phase31_engine_common.mjs';
const info = await runPowerShell('docker info');
const ps = await runPowerShell('docker ps');
const composePs = await runPowerShell('docker compose -f backend\\deployments\\docker-compose\\docker-compose.yml ps');
const composeLogs = await runPowerShell('docker compose -f backend\\deployments\\docker-compose\\docker-compose.yml logs entitlement-service --tail=20');
await writeJson('docker-control-plane-restored-check.json', stamp({ dockerInfo: info, dockerPs: ps, composePs, composeLogs, controlPlaneRestored: info.status === 'ok' && ps.status === 'ok' && composePs.status === 'ok' && composeLogs.status === 'ok' }));
