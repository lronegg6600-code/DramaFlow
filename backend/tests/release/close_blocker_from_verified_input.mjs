import path from "node:path";

import { readJsonIfExists, releaseEvidenceDir, writeJson, writeTicketArtifacts } from "./_blocker_ticket_helpers.mjs";

const ticketMap = readJsonIfExists(path.join(releaseEvidenceDir(), "blocker-ticket-map.json"), { items: [] });
const closeoutLog = readJsonIfExists(path.join(releaseEvidenceDir(), "blocker-closeout-log.json"), { items: [] });
const acceptanceLog = readJsonIfExists(path.join(releaseEvidenceDir(), "input-acceptance-log.json"), { items: [] });
const generatedAt = new Date().toISOString();

for (const item of ticketMap.items) {
  if (item.current_status === "received_and_verified" && item.issue_lifecycle_status !== "closed") {
    item.issue_lifecycle_status = "closed";
    item.closed_at = generatedAt;
    closeoutLog.items.push({
      blocker_id: item.blocker_id,
      closed_at: generatedAt,
      reason: "validator returned received_and_verified"
    });
    acceptanceLog.items.push({
      blocker_id: item.blocker_id,
      accepted_at: generatedAt,
      reason: "input package verified"
    });
  }
}

ticketMap.generatedAt = generatedAt;
writeTicketArtifacts(ticketMap);
writeJson(path.join(releaseEvidenceDir(), "blocker-closeout-log.json"), closeoutLog);
writeJson(path.join(releaseEvidenceDir(), "input-acceptance-log.json"), acceptanceLog);

console.log("[close_blocker_from_verified_input] pass");
