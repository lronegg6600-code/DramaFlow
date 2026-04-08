import fs from "node:fs";
import path from "node:path";

import { buildIssueBody, readJsonIfExists, releaseEvidenceDir, writeMarkdown } from "./_blocker_ticket_helpers.mjs";

const payload = readJsonIfExists(path.join(releaseEvidenceDir(), "blocker-ticket-map.json"), { items: [] });
const issueDir = path.join(releaseEvidenceDir(), "blocker-issues");
fs.mkdirSync(issueDir, { recursive: true });

for (const item of payload.items) {
  writeMarkdown(path.join(issueDir, `${item.blocker_id}.md`), `${buildIssueBody(item)}\n`);
}

console.log("[render_blocker_issue_markdown] pass");
