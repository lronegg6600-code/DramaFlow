import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./external_staging_url_common.mjs";

const rerun = (await readJsonIfExists(path.join(EVIDENCE_DIR, "mobile-rerun-after-external-urls.json"))) ?? { rerunExecuted: false, results: [] };
const payload = {
  generatedAt: getNowIso(),
  rerunExecuted: rerun.rerunExecuted,
  artifactCount: rerun.results.length,
  reason: rerun.rerunExecuted ? "Mobile integration artifacts collected from external URL rerun." : "Skipped because external URL rerun did not execute.",
};
await writeJson(path.join(EVIDENCE_DIR, "mobile-integration-resume-summary.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(rerun.rerunExecuted ? 0 : 1);
