import path from "node:path";
import { EVIDENCE_DIR, SOURCE_RECOVERY_DIR, SOURCE_RECOVERY_ITEMS, classifyItem, ensureDir, getNowIso, listFiles, summarizeStatuses, writeJson } from "./source_recovery_common.mjs";

await ensureDir(path.join(SOURCE_RECOVERY_DIR, "repo-root"));
await ensureDir(path.join(SOURCE_RECOVERY_DIR, "android-source"));
await ensureDir(path.join(SOURCE_RECOVERY_DIR, "backend-source"));
await ensureDir(path.join(SOURCE_RECOVERY_DIR, "manifests"));

const items = [];
for (const item of SOURCE_RECOVERY_ITEMS) {
  const files = await listFiles(item.path);
  items.push({ id: item.id, key: item.key, title: item.title, providerRole: item.providerRole, intakePath: item.path, requiredFiles: item.requiredFiles, receivedFiles: files, status: classifyItem(files, item.requiredFiles) });
}
const payload = { generatedAt: getNowIso(), sourceRecoveryDir: SOURCE_RECOVERY_DIR, summary: summarizeStatuses(items), items };
await writeJson(path.join(EVIDENCE_DIR, "source-recovery-intake.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.summary.received_and_verified === SOURCE_RECOVERY_ITEMS.length ? 0 : 1);
