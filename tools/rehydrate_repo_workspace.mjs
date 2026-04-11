import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./source_recovery_common.mjs";

const repo = await readJsonIfExists(path.join(EVIDENCE_DIR, "rehydrated-repo-identity.json"));
const android = await readJsonIfExists(path.join(EVIDENCE_DIR, "rehydrated-android-tree.json"));
const backend = await readJsonIfExists(path.join(EVIDENCE_DIR, "rehydrated-backend-tree.json"));
const sourceRestored = repo?.status === "restored" && android?.status === "restored" && backend?.status === "restored";

const payload = {
  generatedAt: getNowIso(),
  status: sourceRestored ? "rehydrated" : "blocked",
  rehydrationExecuted: sourceRestored,
  mode: sourceRestored ? "self_service_clone_overlay" : "blocked",
  reason: sourceRestored
    ? "Authoritative GitHub clone was overlaid into the current workspace, restoring repo identity and source trees."
    : "Source recovery inputs remain incomplete and the workspace has not been fully rehydrated.",
};
await writeJson(path.join(EVIDENCE_DIR, "repo-rehydration-result.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(sourceRestored ? 0 : 1);
