import path from "node:path";
import { EVIDENCE_DIR, ROOT, getNowIso, pathExists, writeJson } from "./source_recovery_common.mjs";

const payload = { generatedAt: getNowIso(), status: (await pathExists(path.join(ROOT, ".git"))) ? "restored" : "missing", gitPath: path.join(ROOT, ".git") };
await writeJson(path.join(EVIDENCE_DIR, "rehydrated-repo-identity.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.status === "restored" ? 0 : 1);
