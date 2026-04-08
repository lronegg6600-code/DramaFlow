import path from "node:path";

import {
  readJsonIfExists,
  releaseEvidenceDir,
  summarizeLifecycleStatus,
  writeJson,
  writeTicketArtifacts
} from "./_blocker_ticket_helpers.mjs";

const issueManifestFile =
  process.env.DRAMAFLOW_ISSUE_STATUS_MANIFEST ||
  path.join(releaseEvidenceDir(), "issue-status-manifest.json");

const ticketMap = readJsonIfExists(path.join(releaseEvidenceDir(), "blocker-ticket-map.json"), { items: [] });
const manifest = readJsonIfExists(issueManifestFile, { items: [] });
const issueIndex = new Map((manifest.items || []).map((item) => [item.blocker_id, item]));

for (const item of ticketMap.items) {
  const manifestItem = issueIndex.get(item.blocker_id);
  if (!manifestItem) continue;
  item.issue_number = manifestItem.issue_number ?? item.issue_number;
  item.issue_url = manifestItem.issue_url ?? item.issue_url;
  item.issue_lifecycle_status = manifestItem.issue_lifecycle_status ?? item.issue_lifecycle_status;
  item.last_external_update = manifestItem.last_external_update ?? item.last_external_update;
}

ticketMap.generatedAt = new Date().toISOString();
ticketMap.summary = summarizeLifecycleStatus(ticketMap.items);

writeTicketArtifacts(ticketMap);
writeJson(path.join(releaseEvidenceDir(), "issue-status-sync.json"), {
  generatedAt: ticketMap.generatedAt,
  issueManifestFile,
  summary: ticketMap.summary
});
writeJson(path.join(releaseEvidenceDir(), "unblock-ops-summary.json"), {
  generatedAt: ticketMap.generatedAt,
  issueManifestFile,
  readyForExternalExecutionAtScale: true,
  readyForRealStagingExecution: false,
  summary: ticketMap.summary
});

console.log("[sync_issue_state_from_manifest] pass");
