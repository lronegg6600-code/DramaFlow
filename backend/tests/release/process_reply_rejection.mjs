import path from "node:path";

import { appendLog, buildInvalidReplyBody, canUseGithub, execGh, loadTicketMap, saveTicketMap, writeNamedEvidence } from "./_issue_ops_helpers.mjs";
import { readJsonIfExists, releaseEvidenceDir } from "./_blocker_ticket_helpers.mjs";

const mode = String(process.env.DRAMAFLOW_REAL_COMMENTS || "false").toLowerCase() === "true" && canUseGithub() ? "real" : "dry-run";
const ticketMap = loadTicketMap();
const generatedAt = new Date().toISOString();
const rejectionItems = [];
const transitionItems = [];

const validations = [
  "repo_identity_input_package.json",
  "artifact_identity_input_package.json",
  "cluster_access_input_package.json",
  "github_environment_input_package.json",
  "secret_inventory_input_package.json",
  "deploy_tooling_input_package.json"
].map((file) => readJsonIfExists(path.join(releaseEvidenceDir(), file), { items: [] }));

const invalidIds = new Set(
  validations.flatMap((payload) =>
    (payload.items || []).filter((item) => item.current_status === "received_but_invalid").map((item) => item.blocker_id)
  )
);

for (const item of ticketMap.items.filter((entry) => invalidIds.has(entry.blocker_id))) {
  const reason = "validator returned received_but_invalid";
  const body = buildInvalidReplyBody(item, reason);
  if (mode === "real" && item.issue_number) {
    execGh(["issue", "comment", String(item.issue_number), "--repo", process.env.GITHUB_REPOSITORY, "--body", body]);
    appendLog("blocker-comment-log.json", { blocker_id: item.blocker_id, issue_number: item.issue_number, comment_type: "rejected", created_at: generatedAt, body });
  }
  transitionItems.push({
    blocker_id: item.blocker_id,
    from: item.issue_lifecycle_status,
    to: "replied_invalid",
    at: generatedAt
  });
  item.issue_lifecycle_status = "replied_invalid";
  rejectionItems.push({ blocker_id: item.blocker_id, issue_number: item.issue_number, mode, reason });
}

saveTicketMap(ticketMap);
writeNamedEvidence("blocker-rejection-log.json", { generatedAt, mode, items: rejectionItems });
appendTransitionLog(transitionItems, generatedAt);

console.log("[process_reply_rejection] pass");

function appendTransitionLog(items, generatedAt) {
  const file = path.join(releaseEvidenceDir(), "blocker-lifecycle-transition-log.json");
  const current = readJsonIfExists(file, { generatedAt, items: [] });
  current.generatedAt = generatedAt;
  current.items.push(...items);
  writeNamedEvidence("blocker-lifecycle-transition-log.json", current);
}
