import path from "node:path";
import { EVIDENCE_DIR, SOURCE_RECOVERY_ITEMS, getNowIso, readJsonIfExists, writeJson } from "./source_recovery_common.mjs";

const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "source-recovery-validation.json"))) ?? { summary: { not_received: 4, received_but_invalid: 0, received_partial: 0, received_and_verified: 0 }, items: [] };
const ownerStatus = SOURCE_RECOVERY_ITEMS.map((item) => {
  const matched = validation.items.find((entry) => entry.id === item.id);
  return { id: item.id, title: item.title, primaryOwner: item.providerRole, dueBucket: item.dueBucket, status: matched?.status ?? "not_received", acceptanceCriteria: item.acceptanceCriteria, blocksStaging: item.blocksStaging, blocksProduction: item.blocksProduction };
});
const burndown = { generatedAt: getNowIso(), total: SOURCE_RECOVERY_ITEMS.length, receivedAndVerified: validation.summary.received_and_verified ?? 0, remaining: (validation.summary.not_received ?? 0) + (validation.summary.received_but_invalid ?? 0) + (validation.summary.received_partial ?? 0), summary: validation.summary };
await writeJson(path.join(EVIDENCE_DIR, "source-recovery-owner-status.json"), { generatedAt: getNowIso(), items: ownerStatus });
await writeJson(path.join(EVIDENCE_DIR, "source-recovery-burndown.json"), burndown);
await writeJson(path.join(EVIDENCE_DIR, "blocker-ticket-map.json"), { generatedAt: getNowIso(), items: ownerStatus.map((item) => ({ blocker_id: item.id, blocker_title: item.title, issue_type: "source-recovery", labels: ["blocker", "blocker:external", "blocker:source-recovery"], suggested_assignee_role: item.primaryOwner, due_bucket: item.dueBucket, current_status: item.status, close_condition: "received_and_verified", reopen_condition: "received_but_invalid or received_partial after prior verification", issue_number: null, issue_url: null })) });
await writeJson(path.join(EVIDENCE_DIR, "blocker-reminder-queue.json"), { generatedAt: getNowIso(), queue: ownerStatus.filter((item) => item.status === "not_received" || item.status === "received_partial") });
await writeJson(path.join(EVIDENCE_DIR, "blocker-escalation-queue.json"), { generatedAt: getNowIso(), queue: ownerStatus.filter((item) => item.status === "received_but_invalid") });
await writeJson(path.join(EVIDENCE_DIR, "blocker-closeout-log.json"), { generatedAt: getNowIso(), closed: ownerStatus.filter((item) => item.status === "received_and_verified") });
console.log(JSON.stringify({ burndown, ownerStatus }, null, 2));
process.exit(burndown.remaining === 0 ? 0 : 1);
