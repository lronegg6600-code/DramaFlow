import { readJsonIfExists, releaseEvidenceDir, writeMarkdown } from "./_blocker_ticket_helpers.mjs";
import { writeNamedEvidence } from "./_issue_ops_helpers.mjs";
import path from "node:path";

const evidenceDir = releaseEvidenceDir();
const ticketMap = readJsonIfExists(path.join(evidenceDir, "blocker-ticket-map.json"), { items: [], summary: {} });
const launchSummary = readJsonIfExists(path.join(evidenceDir, "blocker-issue-launch-summary.json"), {});
const assignment = readJsonIfExists(path.join(evidenceDir, "blocker-owner-assignment.json"), { items: [] });
const reminders = readJsonIfExists(path.join(evidenceDir, "blocker-first-reminder-log.json"), { items: [] });
const comments = readJsonIfExists(path.join(evidenceDir, "blocker-comment-log.json"), { items: [] });
const generatedAt = new Date().toISOString();

const payload = {
  generatedAt,
  launch_mode: launchSummary.mode || "unknown",
  launch_state: launchSummary.launch_state || "unknown",
  created_count: launchSummary.created_count || 0,
  failed_count: launchSummary.failed_count || 0,
  assigned_count: assignment.items.filter((item) => item.assignment_state === "assigned").length,
  awaiting_reply_count: ticketMap.summary.awaiting_reply || 0,
  replied_invalid_count: ticketMap.summary.replied_invalid || 0,
  verified_count: ticketMap.summary.verified || 0,
  closed_count: ticketMap.summary.closed || 0,
  reminders_dispatched: reminders.items.length,
  comments_written: comments.items.length
};

writeNamedEvidence("live-launch-summary.json", payload);
writeNamedEvidence("blocker-launch-results.json", {
  generatedAt,
  launch_mode: payload.launch_mode,
  launch_state: payload.launch_state,
  created_count: payload.created_count,
  failed_count: payload.failed_count,
  failures: launchSummary.failures || []
});
writeNamedEvidence("ticket-launch-state.json", {
  generatedAt,
  state:
    payload.created_count === 0
      ? "ready_to_launch"
      : payload.failed_count === 0
        ? "real_launch"
        : "partially_launched",
  summary: payload
});

writeMarkdown(
  path.join(evidenceDir, "real-ticket-launch-report.md"),
  `# Real Ticket Launch Report

- generated_at: ${generatedAt}
- launch_mode: ${payload.launch_mode}
- launch_state: ${payload.launch_state}
- created_count: ${payload.created_count}
- failed_count: ${payload.failed_count}
- assigned_count: ${payload.assigned_count}
- awaiting_reply_count: ${payload.awaiting_reply_count}
- reminders_dispatched: ${payload.reminders_dispatched}
- comments_written: ${payload.comments_written}
`
);

console.log("[publish_launch_outcome] pass");
