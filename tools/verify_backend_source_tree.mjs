import { readFile } from "node:fs/promises";
import path from "node:path";

const ROOT = path.resolve("Z:/Projects/DramaFlow");

const audit = JSON.parse(await readFile(path.join(ROOT, "release-evidence", "workspace-layout-audit.json"), "utf8"));
const result = {
  generatedAt: new Date().toISOString(),
  status: audit.backendStatus,
  sourceCount: audit.backendSourceCount,
  hasGoWork: audit.backendHasGoWork,
  hasGoMod: audit.backendHasGoMod,
  hasServicesDir: audit.backendServicesDirExists,
};
console.log(JSON.stringify(result, null, 2));
process.exit(result.status === "recovered" ? 0 : 1);
