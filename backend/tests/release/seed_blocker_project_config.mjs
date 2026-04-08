import fs from "node:fs";
import path from "node:path";

import { rootDir, writeJson } from "./_intake_validation_helpers.mjs";

const configFile = path.join(rootDir(), ".github", "projects", "blocker-project-config.json");
const fieldsFile = path.join(rootDir(), ".github", "projects", "blocker-project-fields.json");
const viewsFile = path.join(rootDir(), ".github", "projects", "blocker-project-views.md");

const outputFile = path.join(rootDir(), "release-evidence", "blocker-project-seed.json");

writeJson(outputFile, {
  generatedAt: new Date().toISOString(),
  configFile,
  fieldsFile,
  viewsFile,
  config: JSON.parse(fs.readFileSync(configFile, "utf8")),
  fields: JSON.parse(fs.readFileSync(fieldsFile, "utf8"))
});

console.log("[seed_blocker_project_config] pass");
