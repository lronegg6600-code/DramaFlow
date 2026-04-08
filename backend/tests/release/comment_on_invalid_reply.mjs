import { appendLog, buildInvalidReplyBody, canUseGithub, execGh, loadTicketMap, saveTicketMap, writeNamedEvidence } from "./_issue_ops_helpers.mjs";

const mode = String(process.env.DRAMAFLOW_REAL_COMMENTS || "false").toLowerCase() === "true" && canUseGithub() ? "real" : "dry-run";
const ticketMap = loadTicketMap();
const generatedAt = new Date().toISOString();
const items = [];

for (const item of ticketMap.items.filter((entry) => entry.current_status === "received_but_invalid" && entry.issue_number)) {
  const reason = "validator returned received_but_invalid";
  const body = buildInvalidReplyBody(item, reason);
  if (mode === "real") {
    execGh(["issue", "comment", String(item.issue_number), "--repo", process.env.GITHUB_REPOSITORY, "--body", body]);
    appendLog("blocker-comment-log.json", { blocker_id: item.blocker_id, issue_number: item.issue_number, comment_type: "invalid_reply", created_at: generatedAt, body });
  }
  item.issue_lifecycle_status = "replied_invalid";
  items.push({ blocker_id: item.blocker_id, issue_number: item.issue_number, reason, mode });
}

saveTicketMap(ticketMap);
writeNamedEvidence("blocker-reopen-log.json", { generatedAt, mode, items });

console.log("[comment_on_invalid_reply] pass");
