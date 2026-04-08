import path from "node:path";

import { readJsonIfExists, releaseEvidenceDir, writeMarkdown, writeJson } from "./_blocker_ticket_helpers.mjs";

const evidenceDir = releaseEvidenceDir();
const ticketMap = readJsonIfExists(path.join(evidenceDir, "blocker-ticket-map.json"), { items: [], summary: {} });
const replyLog = readJsonIfExists(path.join(evidenceDir, "issue-reply-ingest-log.json"), { totalReplies: 0, items: [] });
const inputLog = readJsonIfExists(path.join(evidenceDir, "new-input-detection-log.json"), { totalNewInputs: 0 });
const validatorLog = readJsonIfExists(path.join(evidenceDir, "validator-run-log.json"), { results: [] });
const acceptanceLog = readJsonIfExists(path.join(evidenceDir, "blocker-acceptance-log.json"), { items: [] });
const rejectionLog = readJsonIfExists(path.join(evidenceDir, "blocker-rejection-log.json"), { items: [] });
const secondReminderLog = readJsonIfExists(path.join(evidenceDir, "blocker-second-reminder-log.json"), { total: 0, dispatched: 0, queued_not_due: 0 });
const escalationLog = readJsonIfExists(path.join(evidenceDir, "blocker-escalation-dispatch-log.json"), { total: 0 });

const generatedAt = new Date().toISOString();
const stagingItems = ticketMap.items.filter((item) => item.blocks_staging === "yes");
const countByState = (items, state) => items.filter((item) => item.issue_lifecycle_status === state).length;

const burndown = {
  generatedAt,
  realReplies: replyLog.totalReplies || 0,
  newInputs: inputLog.totalNewInputs || 0,
  validatorPassed: acceptanceLog.items.length,
  validatorRejected: rejectionLog.items.length,
  lifecycle: {
    awaiting_reply: countByState(ticketMap.items, "awaiting_reply"),
    replied_invalid: countByState(ticketMap.items, "replied_invalid"),
    verified: countByState(ticketMap.items, "verified"),
    closed: countByState(ticketMap.items, "closed"),
    escalated: countByState(ticketMap.items, "escalated")
  },
  staging: {
    total: stagingItems.length,
    awaiting_reply: countByState(stagingItems, "awaiting_reply"),
    replied_invalid: countByState(stagingItems, "replied_invalid"),
    verified: countByState(stagingItems, "verified"),
    closed: countByState(stagingItems, "closed"),
    remaining: stagingItems.filter((item) => !["verified", "closed"].includes(item.issue_lifecycle_status)).length
  },
  reminders: {
    secondReminderDispatched: secondReminderLog.dispatched || 0,
    secondReminderQueued: secondReminderLog.queued_not_due || 0,
    escalated: escalationLog.total || 0
  },
  readyForRealStagingGateRecheck:
    stagingItems.filter((item) => !["verified", "closed"].includes(item.issue_lifecycle_status)).length === 0
};

writeJson(path.join(evidenceDir, "blocker-burndown-summary.json"), burndown);
writeJson(path.join(evidenceDir, "staging-gate-recheck.json"), {
  generatedAt,
  readyForRealStagingGateRecheck: burndown.readyForRealStagingGateRecheck,
  staging: burndown.staging,
  nextAction: burndown.readyForRealStagingGateRecheck
    ? "Run real staging gate and staging rehearsal"
    : "Continue waiting on external replies and prioritize remaining staging blockers"
});

writeMarkdown(
  path.join(evidenceDir, "blocker-burndown-report.md"),
  `# Blocker Burn-down Report

- real_replies: ${burndown.realReplies}
- new_inputs: ${burndown.newInputs}
- validator_passed: ${burndown.validatorPassed}
- validator_rejected: ${burndown.validatorRejected}
- awaiting_reply: ${burndown.lifecycle.awaiting_reply}
- replied_invalid: ${burndown.lifecycle.replied_invalid}
- verified: ${burndown.lifecycle.verified}
- closed: ${burndown.lifecycle.closed}
- escalated: ${burndown.lifecycle.escalated}
- staging_remaining: ${burndown.staging.remaining}
- ready_for_real_staging_gate_recheck: ${burndown.readyForRealStagingGateRecheck ? "yes" : "no"}
`
);

console.log("[summarize_burndown] pass");
