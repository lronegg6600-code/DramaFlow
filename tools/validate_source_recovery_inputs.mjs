import path from "node:path";
import { EVIDENCE_DIR, ROOT, SOURCE_RECOVERY_ITEMS, classifyItem, getNowIso, listFiles, summarizeStatuses, writeJson } from "./source_recovery_common.mjs";

const validations = [];
for (const item of SOURCE_RECOVERY_ITEMS) {
  const files = await listFiles(item.path);
  const status = classifyItem(files, item.requiredFiles);
  const missingRequired = item.requiredFiles.filter((file) => !files.includes(file));
  validations.push({ id: item.id, title: item.title, providerRole: item.providerRole, status, files, missingRequired, acceptanceCriteria: item.acceptanceCriteria, verificationCommand: item.id === "SR-001" ? `node ${path.join(ROOT, "tools", "verify_rehydrated_repo_identity.mjs")}` : item.id === "SR-002" ? `node ${path.join(ROOT, "tools", "verify_rehydrated_android_tree.mjs")}` : item.id === "SR-003" ? `node ${path.join(ROOT, "tools", "verify_rehydrated_backend_tree.mjs")}` : `node ${path.join(ROOT, "tools", "rerun_mobile_integration_gate.mjs")}` });
}
const payload = { generatedAt: getNowIso(), summary: summarizeStatuses(validations), items: validations };
await writeJson(path.join(EVIDENCE_DIR, "source-recovery-validation.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.summary.received_and_verified === SOURCE_RECOVERY_ITEMS.length ? 0 : 1);
