import fs from "node:fs";
import path from "node:path";

import { intakeRoot, writeJson } from "./_intake_validation_helpers.mjs";
import { releaseEvidenceDir } from "./_blocker_ticket_helpers.mjs";

const categories = [
  "repo-identity",
  "artifact-identity",
  "cluster-access",
  "github-environments",
  "secrets",
  "deploy-tooling"
];

const generatedAt = new Date().toISOString();
const items = [];

for (const category of categories) {
  const dir = path.join(intakeRoot(), "received", category);
  const files = fs.existsSync(dir)
    ? fs.readdirSync(dir)
        .filter((file) => !file.startsWith(".gitkeep"))
        .map((file) => {
          const fullPath = path.join(dir, file);
          const stat = fs.statSync(fullPath);
          return {
            category,
            file,
            full_path: fullPath,
            size: stat.size,
            modified_at: stat.mtime.toISOString()
          };
        })
    : [];
  items.push({ category, new_input_count: files.length, files });
}

writeJson(path.join(releaseEvidenceDir(), "new-input-detection-log.json"), {
  generatedAt,
  totalCategories: categories.length,
  totalNewInputs: items.reduce((sum, item) => sum + item.new_input_count, 0),
  items
});

console.log("[ingest_reply_inputs] pass");
