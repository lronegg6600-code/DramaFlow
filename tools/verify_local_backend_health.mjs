import path from "node:path";
import { EVIDENCE_DIR, SERVICE_PORTS, checkHealth, getNowIso, writeJson } from "./local_docker_common.mjs";

const items = [];
for (const service of SERVICE_PORTS) {
  const result = await checkHealth(`http://127.0.0.1:${service.port}/health/live`);
  items.push({ service: service.name, port: service.port, healthy: result.ok, status: result.status ?? null, reason: result.ok ? `HTTP ${result.status}` : result.reason });
}
const payload = { generatedAt: getNowIso(), allHealthy: items.every((item) => item.healthy), items };
await writeJson(path.join(EVIDENCE_DIR, "local-backend-health-check.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.allHealthy ? 0 : 1);
