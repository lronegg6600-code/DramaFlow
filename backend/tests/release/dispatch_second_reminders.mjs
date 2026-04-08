import path from "node:path";

import { appendLog, buildReminderBody, canUseGithub, execGh, loadTicketMap, saveTicketMap, writeNamedEvidence } from "./_issue_ops_helpers.mjs";

const mode = String(process.env.DRAMAFLOW_REAL_COMMENTS || "false").toLowerCase() === "true" && canUseGithub() ? "real" : "dry-run";
const ticketMap = loadTicketMap();
const generatedAt = new Date().toISOString();
const thresholds = {
  same_day: 8 * 60 * 60 * 1000,
  next_business_day: 24 * 60 * 60 * 1000,
  two_business_days: 48 * 60 * 60 * 1000
};

const items = [];
for (const item of ticketMap.items.filter((entry) => entry.issue_lifecycle_status === "awaiting_reply")) {
  const firstReminderAt = item.first_reminder_at ? new Date(item.first_reminder_at).getTime() : null;
  const threshold = thresholds[item.due_date_bucket] ?? thresholds.next_business_day;
  const due = firstReminderAt !== null && Date.now() - firstReminderAt >= threshold;
  if (!due) {
    items.push({
      blocker_id: item.blocker_id,
      issue_number: item.issue_number,
      mode,
      status: "queued_not_due",
      due_bucket: item.due_date_bucket
    });
    continue;
  }
  const body = `${buildReminderBody(item)}\n\nThis is the second reminder because the blocker is still awaiting reply.`;
  if (mode === "real" && item.issue_number) {
    execGh(["issue", "comment", String(item.issue_number), "--repo", process.env.GITHUB_REPOSITORY, "--body", body]);
    appendLog("blocker-comment-log.json", { blocker_id: item.blocker_id, issue_number: item.issue_number, comment_type: "second_reminder", created_at: generatedAt, body });
  }
  item.second_reminder_at = generatedAt;
  items.push({
    blocker_id: item.blocker_id,
    issue_number: item.issue_number,
    mode,
    status: "dispatched",
    due_bucket: item.due_date_bucket
  });
}

saveTicketMap(ticketMap);
writeNamedEvidence("blocker-second-reminder-log.json", {
  generatedAt,
  mode,
  total: items.length,
  dispatched: items.filter((item) => item.status === "dispatched").length,
  queued_not_due: items.filter((item) => item.status === "queued_not_due").length,
  items
});

console.log("[dispatch_second_reminders] pass");
