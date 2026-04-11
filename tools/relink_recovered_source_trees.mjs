import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./source_recovery_common.mjs";

const rehydration = await readJsonIfExists(path.join(EVIDENCE_DIR, "repo-rehydration-result.json"));
const payload = {
  generatedAt: getNowIso(),
  action: "no_relink_performed",
  reason: rehydration?.status === "rehydrated"
    ? "Automated relink is intentionally skipped until authoritative source files are locally present."
    : "Repo rehydration is still blocked; relink skipped.",
};
await writeJson(path.join(EVIDENCE_DIR, "workspace-recovery-actions.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(1);
