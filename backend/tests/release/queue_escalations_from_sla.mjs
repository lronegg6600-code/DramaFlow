import path from "node:path";

import { readJsonIfExists, releaseEvidenceDir, writeJson } from "./_blocker_ticket_helpers.mjs";

const ticketMap = readJsonIfExists(path.join(releaseEvidenceDir(), "blocker-ticket-map.json"), { items: [] });
const generatedAt = new Date().toISOString();

const items = ticketMap.items
  .filter((item) => item.issue_lifecycle_status === "replied_invalid" || item.issue_lifecycle_status === "escalated")
  .map((item) => ({
    blocker_id: item.blocker_id,
    owner: item.suggested_assignee_role,
    escalation_reason:
      item.issue_lifecycle_status === "replied_invalid" ? "input invalid after reply" : "manual escalation requested",
    escalation_path: item.escalation_path
  }));

writeJson(path.join(releaseEvidenceDir(), "blocker-escalation-queue.json"), {
  generatedAt,
  total: items.length,
  items
});

writeJson(path.join(releaseEvidenceDir(), "blocker-weekly-escalation-view.json"), {
  generatedAt,
  items
});

console.log("[queue_escalations_from_sla] pass");
