import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./external_staging_url_common.mjs";

const source = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-base-urls-external-validation.json"))) ?? {
  generatedAt: getNowIso(),
  expectedCount: 7,
  statuses: { not_received: 7, received_but_invalid: 0, verified: 0, closed: 0, escalated: 0 },
  allVerified: false,
  items: [],
};

await writeJson(path.join(EVIDENCE_DIR, "staging-external-url-validation.json"), source);
console.log(JSON.stringify(source, null, 2));
process.exit(source.allVerified ? 0 : 1);
