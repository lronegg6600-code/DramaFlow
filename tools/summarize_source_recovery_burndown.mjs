import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./source_recovery_common.mjs";

const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "source-recovery-validation.json"))) ?? { items: [], summary: { not_received: 0, received_but_invalid: 0, received_partial: 0, received_and_verified: 0 } };
const repo = await readJsonIfExists(path.join(EVIDENCE_DIR, "rehydrated-repo-identity.json"));
const android = await readJsonIfExists(path.join(EVIDENCE_DIR, "rehydrated-android-tree.json"));
const backend = await readJsonIfExists(path.join(EVIDENCE_DIR, "rehydrated-backend-tree.json"));
const rehydration = await readJsonIfExists(path.join(EVIDENCE_DIR, "repo-rehydration-result.json"));

const stagingItem = validation.items.find((item) => item.id === "SR-004");
const items = [
  { id: "SR-001", title: "repo identity missing", primaryOwner: "repo admin", dueBucket: "P0-24h", status: repo?.status === "restored" ? "verified" : "not_received", acceptanceCriteria: [".git exists", "git-backed checkout restored"], blocksStaging: true, blocksProduction: true, resolutionMode: repo?.status === "restored" ? "self_service_clone_overlay" : "external_input" },
  { id: "SR-002", title: "android source missing", primaryOwner: "android owner", dueBucket: "P0-24h", status: android?.status === "restored" ? "verified" : android?.status === "partial" ? "received_partial" : "not_received", acceptanceCriteria: ["android/settings.gradle.kts exists", "android source tree restored"], blocksStaging: true, blocksProduction: true, resolutionMode: android?.status === "restored" ? "self_service_clone_overlay" : "external_input" },
  { id: "SR-003", title: "backend source partial/missing", primaryOwner: "backend owner", dueBucket: "P0-24h", status: backend?.status === "restored" ? "verified" : backend?.status === "partial" ? "received_partial" : "not_received", acceptanceCriteria: ["backend/services exists", "go.work or go.mod exists"], blocksStaging: true, blocksProduction: true, resolutionMode: backend?.status === "restored" ? "self_service_clone_overlay" : "external_input" },
  { id: "SR-004", title: "staging base URL missing", primaryOwner: "platform", dueBucket: "P1-48h", status: stagingItem?.status ?? "not_received", acceptanceCriteria: ["all staging base URLs supplied"], blocksStaging: true, blocksProduction: true, resolutionMode: "external_input" },
];

const summary = { not_received: 0, received_but_invalid: 0, received_partial: 0, received_and_verified: 0, verified: 0, closed: 0 };
for (const item of items) {
  summary[item.status] = (summary[item.status] ?? 0) + 1;
}
const verifiedCount = items.filter((item) => item.status === "verified").length;
const payload = {
  generatedAt: getNowIso(),
  total: 4,
  repliesProcessed: 0,
  externalInputsReceived: validation.items.filter((item) => item.files.length > 0).length,
  selfServiceRecovered: verifiedCount,
  statuses: summary,
  repoRootStatus: items[0].status,
  androidSourceStatus: items[1].status,
  backendSourceStatus: items[2].status,
  stagingInputsStatus: items[3].status,
  rehydrationReady: items.every((item) => item.status === "verified" || item.status === "received_and_verified"),
  repoRehydrated: rehydration?.status === "rehydrated",
  gitStatus: repo?.status ?? "missing",
  androidTreeStatus: android?.status ?? "missing",
  backendTreeStatus: backend?.status ?? "missing",
  currentState: rehydration?.status === "rehydrated" && items[3].status !== "verified" && items[3].status !== "received_and_verified" ? "partially_restored" : rehydration?.status === "rehydrated" ? "repo_rehydrated_and_integration_resumed" : "still_waiting_on_external_restore_inputs",
  items,
};
await writeJson(path.join(EVIDENCE_DIR, "source-recovery-owner-status.json"), { generatedAt: payload.generatedAt, items });
await writeJson(path.join(EVIDENCE_DIR, "source-recovery-burndown.json"), payload);
await writeJson(path.join(EVIDENCE_DIR, "source-recovery-staging-readiness.json"), {
  generatedAt: payload.generatedAt,
  rehydrationReady: payload.rehydrationReady,
  repoRehydrated: payload.repoRehydrated,
  readyForAndroidBackendIntegration: payload.repoRehydrated && (items[3].status === "verified" || items[3].status === "received_and_verified"),
  reason: payload.repoRehydrated ? (items[3].status === "verified" || items[3].status === "received_and_verified" ? "Repo restored and staging inputs verified." : "Repo restored, but staging inputs are still missing.") : "Source recovery still incomplete.",
  missingOwners: items.filter((item) => item.status !== "verified" && item.status !== "received_and_verified").map((item) => ({ id: item.id, providerRole: item.primaryOwner, status: item.status })),
});
await writeJson(path.join(EVIDENCE_DIR, "blocker-ticket-map.json"), { generatedAt: payload.generatedAt, items: items.map((item) => ({ blocker_id: item.id, blocker_title: item.title, issue_type: "source-recovery", labels: ["blocker", "blocker:external", "blocker:source-recovery"], suggested_assignee_role: item.primaryOwner, due_bucket: item.dueBucket, current_status: item.status, close_condition: "verified then closed", reopen_condition: "invalidated after verification", issue_number: null, issue_url: null, resolution_mode: item.resolutionMode })) });
await writeJson(path.join(EVIDENCE_DIR, "blocker-reminder-queue.json"), { generatedAt: payload.generatedAt, queue: items.filter((item) => ["not_received", "received_partial"].includes(item.status)) });
await writeJson(path.join(EVIDENCE_DIR, "blocker-escalation-queue.json"), { generatedAt: payload.generatedAt, queue: items.filter((item) => item.status === "received_but_invalid" || item.status === "escalated") });
await writeJson(path.join(EVIDENCE_DIR, "blocker-closeout-log.json"), { generatedAt: payload.generatedAt, closed: items.filter((item) => item.status === "closed") });
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.repoRehydrated ? 0 : 1);
