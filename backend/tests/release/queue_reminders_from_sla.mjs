import path from "node:path";

import { readJsonIfExists, releaseEvidenceDir, writeJson } from "./_blocker_ticket_helpers.mjs";

const ticketMap = readJsonIfExists(path.join(releaseEvidenceDir(), "blocker-ticket-map.json"), { items: [] });
const generatedAt = new Date().toISOString();

const items = ticketMap.items
  .filter((item) => item.current_status !== "received_and_verified")
  .map((item) => {
    let reminder_type = "review_status";
    let next_action = "review blocker state";

    if (item.issue_lifecycle_status === "not_created") {
      reminder_type = "create_ticket";
      next_action = "create blocker issue and notify owner";
    } else if (item.issue_lifecycle_status === "created_unassigned") {
      reminder_type = "assign_owner";
      next_action = "assign ticket to owner and move to Assigned";
    } else if (item.issue_lifecycle_status === "assigned") {
      reminder_type = "request_reply";
      next_action = "request provider input and move to Awaiting Reply";
    } else if (item.issue_lifecycle_status === "awaiting_reply") {
      reminder_type = "follow_up";
      next_action = "follow up on outstanding input before SLA breach";
    } else if (item.issue_lifecycle_status === "replied_invalid") {
      reminder_type = "request_rework";
      next_action = "send invalid reply feedback and request corrected package";
    }

    return {
      blocker_id: item.blocker_id,
      owner: item.suggested_assignee_role,
      reminder_type,
      due_bucket: item.due_date_bucket,
      template: item.issue_form,
      issue_lifecycle_status: item.issue_lifecycle_status,
      next_action
    };
  });

writeJson(path.join(releaseEvidenceDir(), "blocker-reminder-queue.json"), {
  generatedAt,
  total: items.length,
  items
});

writeJson(path.join(releaseEvidenceDir(), "blocker-daily-view.json"), {
  generatedAt,
  focus: "same_day blockers first",
  items: items.filter((item) => item.due_bucket === "same_day")
});

console.log("[queue_reminders_from_sla] pass");
