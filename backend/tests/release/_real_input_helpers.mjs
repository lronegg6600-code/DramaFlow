import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

export function rootDir() {
  return path.resolve(import.meta.dirname, "..", "..", "..");
}

export function evidenceDir() {
  const root = rootDir();
  return process.env.DRAMAFLOW_EVIDENCE_DIR
    ? path.resolve(process.env.DRAMAFLOW_EVIDENCE_DIR)
    : path.join(root, "release-evidence");
}

export function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

export function writeJson(file, payload) {
  ensureDir(path.dirname(file));
  fs.writeFileSync(file, `${JSON.stringify(payload, null, 2)}\n`, "utf8");
}

export function readJson(file) {
  return JSON.parse(fs.readFileSync(file, "utf8"));
}

export function run(command, args, cwd = rootDir()) {
  try {
    return execFileSync(command, args, {
      cwd,
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"]
    }).trim();
  } catch {
    return null;
  }
}

export function item(name, state, details, owner, blocksStaging = true, blocksProduction = true) {
  return { name, state, details, owner, blocksStaging, blocksProduction };
}

export function summarize(items) {
  const missing = items.filter((entry) => entry.state === "missing").length;
  const invalid = items.filter((entry) => entry.state === "invalid").length;
  const verified = items.filter((entry) => entry.state === "verified").length;
  const blockerCount = items.filter((entry) => entry.state !== "verified" && (entry.blocksStaging || entry.blocksProduction)).length;
  const status = blockerCount === 0 ? "verified" : "blocker";
  return { missing, invalid, verified, blockerCount, status };
}

export function printAndExit(label, payload) {
  if (payload.summary.status !== "verified") {
    console.error(`[${label}] blocker`);
    for (const entry of payload.items.filter((item) => item.state !== "verified")) {
      console.error(`- ${entry.name}: ${entry.state} (${entry.details})`);
    }
    process.exit(1);
  }
  console.log(`[${label}] pass`);
}
