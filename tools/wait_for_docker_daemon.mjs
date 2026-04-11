import path from "node:path";
import { EVIDENCE_DIR, getNowIso, waitForDocker, writeJson } from "./local_docker_common.mjs";

const result = await waitForDocker({ attempts: 6, sleepSeconds: 5 });
const payload = { generatedAt: getNowIso(), ready: result.ready, attempts: result.attempts };
await writeJson(path.join(EVIDENCE_DIR, "docker-daemon-readiness.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(result.ready ? 0 : 1);
