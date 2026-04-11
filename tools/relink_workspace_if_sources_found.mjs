import { readFile, writeFile } from "node:fs/promises";
import path from "node:path";

const ROOT = path.resolve("Z:/Projects/DramaFlow");
const audit = JSON.parse(await readFile(path.join(ROOT, "release-evidence", "workspace-layout-audit.json"), "utf8"));

const recommendation = {
  generatedAt: new Date().toISOString(),
  action: "no_relink_performed",
  reason:
    audit.workspaceStatus === "recovered"
      ? "Workspace already recovered; no relink action required."
      : "No complete Android/backend source-of-truth candidate was found in current scan scope.",
  androidCandidates: audit.androidSourceCandidates,
  backendCandidates: audit.backendSourceCandidates,
};

await writeFile(path.join(ROOT, "release-evidence", "workspace-recovery-actions.json"), `${JSON.stringify(recommendation, null, 2)}\n`, "utf8");
console.log(JSON.stringify(recommendation, null, 2));
process.exit(0);
