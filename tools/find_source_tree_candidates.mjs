import { readFile } from "node:fs/promises";
import { spawn } from "node:child_process";
import path from "node:path";

const ROOT = path.resolve("Z:/Projects/DramaFlow");
const EVIDENCE_DIR = path.join(ROOT, "release-evidence");

function runNode(scriptPath) {
  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, [scriptPath], { cwd: ROOT, stdio: ["ignore", "ignore", "pipe"] });
    let stderr = "";
    child.stderr.on("data", (chunk) => (stderr += chunk.toString()));
    child.on("close", (code) => {
      if (code === 0 || code === 1) {
        resolve();
      } else {
        reject(new Error(stderr || `Failed to run ${scriptPath}`));
      }
    });
  });
}

async function main() {
  const auditPath = path.join(EVIDENCE_DIR, "workspace-layout-audit.json");
  try {
    await readFile(auditPath, "utf8");
  } catch {
    await runNode(path.join(ROOT, "tools", "audit_workspace_layout.mjs"));
  }
  const audit = JSON.parse(await readFile(auditPath, "utf8"));
  const payload = {
    generatedAt: new Date().toISOString(),
    androidCandidates: audit.androidSourceCandidates,
    backendCandidates: audit.backendSourceCandidates,
    summary:
      audit.androidSourceCandidates.length === 0 && audit.backendSourceCandidates.length === 0
        ? "No alternate source tree candidates found in current scan scope."
        : "Source tree candidates found; inspect path completeness before relinking.",
  };
  console.log(JSON.stringify(payload, null, 2));
}

await main();
