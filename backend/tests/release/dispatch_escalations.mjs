import { loadTicketMap, writeNamedEvidence } from "./_issue_ops_helpers.mjs";

const generatedAt = new Date().toISOString();
const thresholds = {
  same_day: 12 * 60 * 60 * 1000,
  next_business_day: 36 * 60 * 60 * 1000,
  two_business_days: 72 * 60 * 60 * 1000
};

const ticketMap = loadTicketMap();
const items = [];

for (const item of ticketMap.items.filter((entry) => entry.issue_lifecycle_status === "awaiting_reply")) {
  const firstReminderAt = item.first_reminder_at ? new Date(item.first_reminder_at).getTime() : null;
  const threshold = thresholds[item.due_date_bucket] ?? thresholds.next_business_day;
  const due = firstReminderAt !== null && Date.now() - firstReminderAt >= threshold;
  if (due) {
    item.issue_lifecycle_status = "escalated";
    items.push({
      blocker_id: item.blocker_id,
      issue_number: item.issue_number,
      escalation_path: item.escalation_path,
      status: "escalated"
    });
  }
}

writeNamedEvidence("blocker-escalation-dispatch-log.json", {
  generatedAt,
  total: items.length,
  items
});

console.log("[dispatch_escalations] pass");
