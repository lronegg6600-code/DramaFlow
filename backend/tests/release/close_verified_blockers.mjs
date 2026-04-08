import path from "node:path";

import { appendLog, canUseGithub, execGh, loadTicketMap, saveTicketMap, writeNamedEvidence } from "./_issue_ops_helpers.mjs";
import { readJsonIfExists, releaseEvidenceDir } from "./_blocker_ticket_helpers.mjs";

const mode = String(process.env.DRAMAFLOW_REAL_COMMENTS || "false").toLowerCase() === "true" && canUseGithub() ? "real" : "dry-run";
const ticketMap = loadTicketMap();
const generatedAt = new Date().toISOString();
const closedItems = [];
const transitionItems = [];

for (const item of ticketMap.items.filter((entry) => entry.issue_lifecycle_status === "verified")) {
  if (mode === "real" && item.issue_number) {
    execGh(["issue", "close", String(item.issue_number), "--repo", process.env.GITHUB_REPOSITORY, "--comment", "Validator checks passed. Closing blocker issue."]);
    appendLog("blocker-comment-log.json", { blocker_id: item.blocker_id, issue_number: item.issue_number, comment_type: "closed", created_at: generatedAt, body: "Validator checks passed. Closing blocker issue." });
  }
  transitionItems.push({ blocker_id: item.blocker_id, from: "verified", to: "closed", at: generatedAt });
  item.issue_lifecycle_status = "closed";
  closedItems.push({ blocker_id: item.blocker_id, issue_number: item.issue_number, mode });
}

saveTicketMap(ticketMap);
writeNamedEvidence("blocker-closeout-log.json", { generatedAt, mode, items: closedItems });
appendTransitionLog(transitionItems, generatedAt);

console.log("[close_verified_blockers] pass");

function appendTransitionLog(items, generatedAt) {
  const file = path.join(releaseEvidenceDir(), "blocker-lifecycle-transition-log.json");
  const current = readJsonIfExists(file, { generatedAt, items: [] });
  current.generatedAt = generatedAt;
  current.items.push(...items);
  writeNamedEvidence("blocker-lifecycle-transition-log.json", current);
}
