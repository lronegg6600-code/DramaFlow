import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./source_recovery_common.mjs";

const ownerStatus = (await readJsonIfExists(path.join(EVIDENCE_DIR, "source-recovery-owner-status.json")))?.items ?? [];
const verified = ownerStatus.filter((item) => item.status === "verified" || item.status === "received_and_verified");
const payload = {
  generatedAt: getNowIso(),
  verifiedCount: verified.length,
  items: verified,
  commentDispatchCount: 0,
  reason: verified.length === 0 ? "No verified source recovery item available." : "Verified source recovery items logged; live issue comment dispatch not executed in current environment.",
};
await writeJson(path.join(EVIDENCE_DIR, "source-recovery-verified-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(verified.length === 0 ? 1 : 0);
