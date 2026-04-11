import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./source_recovery_common.mjs";

const ownerStatus = (await readJsonIfExists(path.join(EVIDENCE_DIR, "source-recovery-owner-status.json")))?.items ?? [];
const accepted = ownerStatus.filter((item) => item.status === "verified" || item.status === "received_and_verified");
const payload = {
  generatedAt: getNowIso(),
  acceptedCount: accepted.length,
  items: accepted,
  commentDispatchCount: 0,
  reason: accepted.length === 0 ? "No verified source recovery input available for acceptance." : "Verified items accepted locally; live issue commenting not executed in current environment.",
};
await writeJson(path.join(EVIDENCE_DIR, "source-recovery-acceptance-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(accepted.length === 0 ? 1 : 0);
