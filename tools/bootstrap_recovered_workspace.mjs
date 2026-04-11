import { readFile, writeFile } from "node:fs/promises";
import path from "node:path";

const ROOT = path.resolve("Z:/Projects/DramaFlow");
const audit = JSON.parse(await readFile(path.join(ROOT, "release-evidence", "workspace-layout-audit.json"), "utf8"));

const payload = {
  generatedAt: new Date().toISOString(),
  status: audit.workspaceStatus === "recovered" ? "bootstrapped" : "blocked",
  actions:
    audit.workspaceStatus === "recovered"
      ? ["Re-run mobile readiness scripts", "Reattach docs/release-evidence/platform-intake if needed"]
      : [
          "Restore git-backed checkout",
          "Restore Android source tree",
          "Restore backend services tree",
          "Inject staging base URLs",
        ],
};

await writeFile(path.join(ROOT, "release-evidence", "workspace-recovery-status.json"), `${JSON.stringify(payload, null, 2)}\n`, "utf8");
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.status === "bootstrapped" ? 0 : 1);
