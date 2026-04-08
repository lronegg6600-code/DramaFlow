import path from "node:path";

import { readJsonIfExists, releaseEvidenceDir, writeJson, writeTicketArtifacts } from "./_blocker_ticket_helpers.mjs";

const ticketMap = readJsonIfExists(path.join(releaseEvidenceDir(), "blocker-ticket-map.json"), { items: [] });
const rejectionLog = readJsonIfExists(path.join(releaseEvidenceDir(), "input-rejection-log.json"), { items: [] });
const generatedAt = new Date().toISOString();

for (const item of ticketMap.items) {
  if (item.current_status === "received_but_invalid") {
    item.issue_lifecycle_status = "replied_invalid";
    item.reopened_at = generatedAt;
    rejectionLog.items.push({
      blocker_id: item.blocker_id,
      rejected_at: generatedAt,
      reason: "validator returned received_but_invalid"
    });
  }
}

ticketMap.generatedAt = generatedAt;
writeTicketArtifacts(ticketMap);
writeJson(path.join(releaseEvidenceDir(), "input-rejection-log.json"), rejectionLog);

console.log("[reopen_blocker_from_invalid_input] pass");
