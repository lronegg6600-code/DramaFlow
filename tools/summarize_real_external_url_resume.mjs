import path from "node:path";
import {
  EVIDENCE_DIR,
  getNowIso,
  readJsonIfExists,
  writeJson,
} from "./real_external_url_common.mjs";

const replyLog = await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-real-external-url-reply-log.json"));
const validation = await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-external-url-second-validation.json"));
const dns = await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-real-external-url-dns-check.json"));
const reachability = await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-real-external-url-reachability-check.json"));
const paths = await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-real-external-url-path-check.json"));
const envExport = await readJsonIfExists(path.join(EVIDENCE_DIR, "android-staging-env-verified-export.json"));
const rerun = await readJsonIfExists(path.join(EVIDENCE_DIR, "mobile-rerun-with-verified-external-urls.json"));

const payload = {
  generatedAt: getNowIso(),
  platformReplyCount: replyLog?.platformReplyCount ?? 0,
  verifiedCount: validation?.statuses?.verified ?? 0,
  invalidCount: validation?.statuses?.real_url_received_but_invalid ?? 0,
  notReceivedCount: validation?.statuses?.real_url_not_received ?? 7,
  dnsVerifiedCount: dns?.verifiedCount ?? 0,
  reachableCount: reachability?.reachableCount ?? 0,
  pathOkCount: paths?.pathOkCount ?? 0,
  verifiedEnvExported: envExport?.exported ?? false,
  rerunExecuted: rerun?.rerunExecuted ?? false,
  currentState:
    rerun?.rerunExecuted
      ? "integration_rerun_executed"
      : (validation?.statuses?.verified ?? 0) === 7
        ? "verified_waiting_for_rerun"
        : (validation?.statuses?.real_url_received_but_invalid ?? 0) > 0
          ? "partially_unblocked_but_invalid"
          : "still_blocked_by_unresolved_real_external_staging_urls",
};

await writeJson(path.join(EVIDENCE_DIR, "mobile-integration-resume-summary.json"), payload);
console.log(JSON.stringify(payload, null, 2));
