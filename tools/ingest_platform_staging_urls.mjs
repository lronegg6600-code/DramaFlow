import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./external_staging_url_common.mjs";

const source = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-base-urls-external-intake.json"))) ?? {
  generatedAt: getNowIso(),
  receivedFile: null,
  manifestFiles: [],
  urlFiles: [],
  receivedCount: 0,
  expectedCount: 7,
  items: [],
};

await writeJson(path.join(EVIDENCE_DIR, "staging-base-urls-external-intake.json"), source);
console.log(JSON.stringify(source, null, 2));
process.exit(source.receivedCount === 0 ? 1 : 0);
