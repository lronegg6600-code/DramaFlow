import path from "node:path";

import { appendLog, buildReopenedBody, canUseGithub, execGh, loadTicketMap, saveTicketMap, writeNamedEvidence } from "./_issue_ops_helpers.mjs";
import { readJsonIfExists, releaseEvidenceDir } from "./_blocker_ticket_helpers.mjs";

const mode = String(process.env.DRAMAFLOW_REAL_COMMENTS || "false").toLowerCase() === "true" && canUseGithub() ? "real" : "dry-run";
const ticketMap = loadTicketMap();
const generatedAt = new Date().toISOString();
const reopenedItems = [];
const transitionItems = [];

for (const item of ticketMap.items.filter((entry) => entry.issue_lifecycle_status === "closed" && entry.current_status === "received_but_invalid")) {
  const body = buildReopenedBody(item, "validator returned received_but_invalid after close");
  if (mode === "real" && item.issue_number) {
    execGh(["issue", "reopen", String(item.issue_number), "--repo", process.env.GITHUB_REPOSITORY]);
    execGh(["issue", "comment", String(item.issue_number), "--repo", process.env.GITHUB_REPOSITORY, "--body", body]);
    appendLog("blocker-comment-log.json", { blocker_id: item.blocker_id, issue_number: item.issue_number, comment_type: "reopened", created_at: generatedAt, body });
  }
  transitionItems.push({ blocker_id: item.blocker_id, from: "closed", to: "replied_invalid", at: generatedAt });
  item.issue_lifecycle_status = "replied_invalid";
  reopenedItems.push({ blocker_id: item.blocker_id, issue_number: item.issue_number, mode });
}

saveTicketMap(ticketMap);
writeNamedEvidence("blocker-reopen-log.json", { generatedAt, mode, items: reopenedItems });
appendTransitionLog(transitionItems, generatedAt);

console.log("[reopen_invalidated_blockers] pass");

function appendTransitionLog(items, generatedAt) {
  const file = path.join(releaseEvidenceDir(), "blocker-lifecycle-transition-log.json");
  const current = readJsonIfExists(file, { generatedAt, items: [] });
  current.generatedAt = generatedAt;
  current.items.push(...items);
  writeNamedEvidence("blocker-lifecycle-transition-log.json", current);
}
