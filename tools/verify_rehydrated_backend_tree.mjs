import path from "node:path";
import { EVIDENCE_DIR, ROOT, getNowIso, pathExists, writeJson } from "./source_recovery_common.mjs";

const checks = { servicesDir: await pathExists(path.join(ROOT, "backend", "services")), goWork: await pathExists(path.join(ROOT, "backend", "go.work")), goMod: await pathExists(path.join(ROOT, "backend", "go.mod")) };
const restored = checks.servicesDir && (checks.goWork || checks.goMod);
const partial = Object.values(checks).some(Boolean) && !restored;
const payload = { generatedAt: getNowIso(), status: restored ? "restored" : partial ? "partial" : "missing", checks };
await writeJson(path.join(EVIDENCE_DIR, "rehydrated-backend-tree.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.status === "restored" ? 0 : 1);
