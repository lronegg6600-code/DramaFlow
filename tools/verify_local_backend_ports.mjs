import path from "node:path";
import { EVIDENCE_DIR, SERVICE_PORTS, checkPort, getNowIso, writeJson } from "./local_docker_common.mjs";

const items = [];
for (const service of SERVICE_PORTS) {
  const result = await checkPort(service.port);
  items.push({ service: service.name, port: service.port, reachable: result.ok, reason: result.ok ? "tcp_connect_ok" : result.reason });
}
const payload = { generatedAt: getNowIso(), allReachable: items.every((item) => item.reachable), items };
await writeJson(path.join(EVIDENCE_DIR, "local-backend-port-check.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.allReachable ? 0 : 1);
