import path from "node:path";
import { EVIDENCE_DIR, ROOT, STAGING_INPUT_ITEMS, getNowIso, readJsonIfExists, writeJson, writeText } from "./staging_input_common.mjs";

const validation = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-input-validation.json"))) ?? { items: [] };
const verified = new Map(validation.items.filter((item) => item.status === "verified").map((item) => [item.key, item.providedValue]));
const allVerified = STAGING_INPUT_ITEMS.every((item) => verified.has(item.key));
const envLines = STAGING_INPUT_ITEMS.map((item) => `${item.envVar}=${verified.get(item.key) ?? ""}`);
const envFile = path.join(ROOT, ".env.staging.mobile");
if (allVerified) {
  await writeText(envFile, envLines.join("\n"));
}
const payload = {
  generatedAt: getNowIso(),
  envFile: allVerified ? path.relative(ROOT, envFile) : null,
  exported: allVerified,
  verifiedCount: verified.size,
  missingKeys: STAGING_INPUT_ITEMS.filter((item) => !verified.has(item.key)).map((item) => item.key),
  reason: allVerified ? "Staging env file exported for mobile integration rerun." : "Skipped because staging inputs are incomplete.",
};
await writeJson(path.join(EVIDENCE_DIR, "staging-env-export.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(allVerified ? 0 : 1);
