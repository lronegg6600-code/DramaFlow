import { spawn } from "node:child_process";
import { readFile, writeFile } from "node:fs/promises";
import path from "node:path";

const ROOT = path.resolve("Z:/Projects/DramaFlow");
const audit = JSON.parse(await readFile(path.join(ROOT, "release-evidence", "workspace-layout-audit.json"), "utf8"));

function runNode(scriptPath) {
  return new Promise((resolve) => {
    const child = spawn(process.execPath, [scriptPath], { cwd: ROOT, stdio: ["ignore", "pipe", "pipe"] });
    let stdout = "";
    let stderr = "";
    child.stdout.on("data", (chunk) => (stdout += chunk.toString()));
    child.stderr.on("data", (chunk) => (stderr += chunk.toString()));
    child.on("close", (code) => resolve({ code, stdout, stderr }));
  });
}

const result =
  audit.workspaceStatus === "recovered"
    ? await runNode(path.join(ROOT, "backend", "tests", "integration", "mobile_backend_contract_smoke.mjs"))
    : {
        code: 1,
        stdout: "",
        stderr: "Workspace not recovered; mobile readiness re-run skipped.",
      };

const payload = {
  generatedAt: new Date().toISOString(),
  rerunExecuted: audit.workspaceStatus === "recovered",
  workspaceStatus: audit.workspaceStatus,
  result,
};

await writeFile(path.join(ROOT, "release-evidence", "recovered-mobile-readiness.json"), `${JSON.stringify(payload, null, 2)}\n`, "utf8");
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.rerunExecuted && payload.result.code === 0 ? 0 : 1);
