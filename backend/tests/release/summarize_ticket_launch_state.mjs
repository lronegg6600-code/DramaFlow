import path from "node:path";

import { readJsonIfExists, releaseEvidenceDir, writeJson } from "./_blocker_ticket_helpers.mjs";

const evidenceDir = releaseEvidenceDir();
const ticketMap = readJsonIfExists(path.join(evidenceDir, "blocker-ticket-map.json"), { items: [] });
const launchSummary = readJsonIfExists(path.join(evidenceDir, "blocker-issue-launch-summary.json"), {});

const summary = {
  generatedAt: new Date().toISOString(),
  total: ticketMap.items.length,
  launch_state: launchSummary.launch_state || "launch_not_started",
  ready_to_launch_tickets: Boolean(ticketMap.items.length),
  tickets_launched: ticketMap.items.some((item) => item.issue_number),
  partially_launched:
    ticketMap.items.some((item) => item.issue_number) &&
    ticketMap.items.some((item) => !item.issue_number),
  not_created: ticketMap.items.filter((item) => item.issue_lifecycle_status === "not_created").length,
  created_unassigned: ticketMap.items.filter((item) => item.issue_lifecycle_status === "created_unassigned").length,
  assigned: ticketMap.items.filter((item) => item.issue_lifecycle_status === "assigned").length,
  awaiting_reply: ticketMap.items.filter((item) => item.issue_lifecycle_status === "awaiting_reply").length,
  replied_invalid: ticketMap.items.filter((item) => item.issue_lifecycle_status === "replied_invalid").length,
  verified: ticketMap.items.filter((item) => item.issue_lifecycle_status === "verified").length,
  closed: ticketMap.items.filter((item) => item.issue_lifecycle_status === "closed").length
};

writeJson(path.join(evidenceDir, "unblock-ops-summary.json"), {
  generatedAt: summary.generatedAt,
  readyForExternalExecutionAtScale: true,
  readyForRealStagingExecution: false,
  launch_state: summary.launch_state,
  summary
});

console.log("[summarize_ticket_launch_state] pass");
