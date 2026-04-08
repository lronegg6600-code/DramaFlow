import { appendLog, buildReminderBody, canUseGithub, execGh, loadTicketMap, saveTicketMap, writeNamedEvidence } from "./_issue_ops_helpers.mjs";

const mode = String(process.env.DRAMAFLOW_REAL_COMMENTS || "false").toLowerCase() === "true" && canUseGithub() ? "real" : "dry-run";
const ticketMap = loadTicketMap();
const generatedAt = new Date().toISOString();
const items = [];

for (const item of ticketMap.items.filter((entry) => entry.issue_number && entry.issue_lifecycle_status === "assigned")) {
  const body = buildReminderBody(item);
  const record = {
    blocker_id: item.blocker_id,
    issue_number: item.issue_number,
    issue_url: item.issue_url,
    mode,
    comment_type: "first_reminder",
    created_at: generatedAt
  };
  if (mode === "real") {
    execGh(["issue", "comment", String(item.issue_number), "--repo", process.env.GITHUB_REPOSITORY, "--body", body]);
    appendLog("blocker-comment-log.json", { ...record, body });
  } else {
    record.preview = body;
  }
  item.issue_lifecycle_status = "awaiting_reply";
  item.first_reminder_at = generatedAt;
  item.reminder_state = "dispatched";
  items.push(record);
}

saveTicketMap(ticketMap);
writeNamedEvidence("blocker-first-reminder-log.json", { generatedAt, mode, total: items.length, items });

console.log("[dispatch_first_reminders] pass");
