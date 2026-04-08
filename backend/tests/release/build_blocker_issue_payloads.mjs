import path from "node:path";

import {
  buildTicketRecord,
  loadOwnerMatrix,
  releaseEvidenceDir,
  summarizeLifecycleStatus,
  writeJson,
  writeTicketArtifacts
} from "./_blocker_ticket_helpers.mjs";

const blockers = loadOwnerMatrix();
const items = blockers.map((blocker) => buildTicketRecord(blocker));
const summary = summarizeLifecycleStatus(items);

const payload = {
  generatedAt: new Date().toISOString(),
  items,
  summary
};

writeTicketArtifacts(payload);
writeJson(path.join(releaseEvidenceDir(), "blocker-closeout-log.json"), { generatedAt: payload.generatedAt, items: [] });
writeJson(path.join(releaseEvidenceDir(), "input-acceptance-log.json"), { generatedAt: payload.generatedAt, items: [] });
writeJson(path.join(releaseEvidenceDir(), "input-rejection-log.json"), { generatedAt: payload.generatedAt, items: [] });

console.log("[build_blocker_issue_payloads] pass");
