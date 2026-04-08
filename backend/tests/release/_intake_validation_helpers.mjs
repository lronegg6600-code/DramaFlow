import fs from "node:fs";
import path from "node:path";

export function rootDir() {
  return path.resolve(import.meta.dirname, "..", "..", "..");
}

export function intakeRoot() {
  return path.join(rootDir(), "platform-intake");
}

export function evidenceDir() {
  return process.env.DRAMAFLOW_EVIDENCE_DIR
    ? path.resolve(process.env.DRAMAFLOW_EVIDENCE_DIR)
    : path.join(rootDir(), "release-evidence");
}

export function validationDir() {
  return path.join(intakeRoot(), "validation-results");
}

export function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

export function writeJson(file, payload) {
  ensureDir(path.dirname(file));
  fs.writeFileSync(file, `${JSON.stringify(payload, null, 2)}\n`, "utf8");
}

export function placeholder(value) {
  if (!value) return true;
  const normalized = String(value).trim().toLowerCase();
  return (
    normalized === "" ||
    normalized.includes("<") ||
    normalized.includes("replace") ||
    normalized.includes("placeholder") ||
    normalized === "tbd" ||
    normalized === "pending"
  );
}

export function listCandidateFiles(dir) {
  if (!fs.existsSync(dir)) return [];
  return fs
    .readdirSync(dir)
    .filter((file) => file.endsWith(".yaml") || file.endsWith(".yml") || file.endsWith(".json"))
    .map((file) => path.join(dir, file));
}

export function parseLooseConfig(file) {
  const raw = fs.readFileSync(file, "utf8");
  if (raw.trim().startsWith("{")) {
    return JSON.parse(raw);
  }

  const data = {};
  for (const line of raw.split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const idx = trimmed.indexOf(":");
    if (idx === -1) continue;
    const key = trimmed.slice(0, idx).trim();
    let value = trimmed.slice(idx + 1).trim();
    value = value.replace(/^['"]|['"]$/g, "");
    data[key] = value;
  }
  return data;
}

export function statusForValue(value) {
  if (value === undefined || value === null || String(value).trim() === "") return "not_received";
  if (placeholder(value)) return "received_but_invalid";
  return "received_and_verified";
}

export function boolStatus(value) {
  if (value === undefined || value === null || String(value).trim() === "") return "not_received";
  const normalized = String(value).trim().toLowerCase();
  if (normalized === "true" || normalized === "yes" || normalized === "1" || normalized === "verified") {
    return "received_and_verified";
  }
  return "received_but_invalid";
}

export function summarize(items) {
  const counts = {
    not_received: items.filter((item) => item.current_status === "not_received").length,
    received_but_invalid: items.filter((item) => item.current_status === "received_but_invalid").length,
    received_and_verified: items.filter((item) => item.current_status === "received_and_verified").length
  };
  return {
    ...counts,
    overall_status: counts.not_received === 0 && counts.received_but_invalid === 0 ? "ready" : "blocked"
  };
}

export function validationPayload(category, sourceFile, items) {
  return {
    category,
    generatedAt: new Date().toISOString(),
    sourceFile,
    items,
    summary: summarize(items)
  };
}

export function writeValidationFiles(baseName, payload) {
  const evidenceFile = path.join(evidenceDir(), `${baseName}.json`);
  const validationFile = path.join(validationDir(), `${baseName}.json`);
  writeJson(evidenceFile, payload);
  writeJson(validationFile, payload);
}

export function printValidationResult(label, payload) {
  if (payload.summary.overall_status !== "ready") {
    console.error(`[${label}] blocker`);
    for (const item of payload.items.filter((entry) => entry.current_status !== "received_and_verified")) {
      console.error(`- ${item.blocker_title}: ${item.current_status} (${item.notes})`);
    }
    process.exit(1);
  }
  console.log(`[${label}] pass`);
}
