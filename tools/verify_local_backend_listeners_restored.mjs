import { runPowerShell, writeJson, stamp } from './phase31_engine_common.mjs';
const listeners = await runPowerShell("Get-NetTCPConnection -State Listen | Where-Object { $_.LocalPort -ge 8081 -and $_.LocalPort -le 8087 } | Select-Object LocalAddress,LocalPort,OwningProcess,State | ConvertTo-Json -Depth 4");
const health = await runPowerShell("8081..8087 | ForEach-Object { try { $r = Invoke-WebRequest -UseBasicParsing (\"http://127.0.0.1:{0}/health/live\" -f $_) -TimeoutSec 5; [pscustomobject]@{Port=$_; Status=$r.StatusCode; Body=$r.Content} } catch { [pscustomobject]@{Port=$_; Status='fail'; Error=$_.Exception.Message} } } | ConvertTo-Json -Depth 4", 180000);
let parsedListeners = [];
let parsedHealth = [];
if (listeners.stdout?.trim()) { try { parsedListeners = JSON.parse(listeners.stdout); } catch {} }
if (health.stdout?.trim()) { try { parsedHealth = JSON.parse(health.stdout); } catch {} }
await writeJson('local-backend-listener-check.json', stamp({ listeners: parsedListeners, health: parsedHealth, listenersRestored: Array.isArray(parsedHealth) && parsedHealth.every(x => x.Status === 200), interpretation: Array.isArray(parsedHealth) && parsedHealth.every(x => x.Status === 200) ? 'All localhost listeners 8081-8087 respond with health 200.' : 'One or more localhost listeners are still unavailable.' }));
