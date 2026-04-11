import { access, mkdir, readdir, readFile, writeFile } from "node:fs/promises";
import { constants } from "node:fs";
import path from "node:path";
import { spawn } from "node:child_process";

export const ROOT = path.resolve("Z:/Projects/DramaFlow");
export const EVIDENCE_DIR = path.join(ROOT, "release-evidence");
export const PLATFORM_INTAKE_DIR = path.join(ROOT, "platform-intake");
export const SOURCE_RECOVERY_DIR = path.join(PLATFORM_INTAKE_DIR, "received", "source-recovery");

export const SOURCE_RECOVERY_ITEMS = [
  { id: "SR-001", key: "repo_root", title: "repo identity missing", providerRole: "repo admin", path: path.join(SOURCE_RECOVERY_DIR, "repo-root"), requiredFiles: ["repo-root.manifest.json"], acceptanceCriteria: [".git exists", "git-backed checkout restored"], blocksStaging: true, blocksProduction: true, dueBucket: "P0-24h" },
  { id: "SR-002", key: "android_source", title: "android source missing", providerRole: "android owner", path: path.join(SOURCE_RECOVERY_DIR, "android-source"), requiredFiles: ["android-source.manifest.json"], acceptanceCriteria: ["android/settings.gradle.kts exists", "android source tree restored"], blocksStaging: true, blocksProduction: true, dueBucket: "P0-24h" },
  { id: "SR-003", key: "backend_source", title: "backend source partial/missing", providerRole: "backend owner", path: path.join(SOURCE_RECOVERY_DIR, "backend-source"), requiredFiles: ["backend-source.manifest.json"], acceptanceCriteria: ["backend/services exists", "go.work or go.mod exists"], blocksStaging: true, blocksProduction: true, dueBucket: "P0-24h" },
  { id: "SR-004", key: "staging_inputs", title: "staging base URL missing", providerRole: "platform", path: path.join(SOURCE_RECOVERY_DIR, "manifests"), requiredFiles: ["source-recovery.manifest.json"], acceptanceCriteria: ["all staging base URLs supplied"], blocksStaging: true, blocksProduction: true, dueBucket: "P1-48h" },
];

export async function pathExists(target) {
  try { await access(target, constants.F_OK); return true; } catch { return false; }
}
export async function ensureDir(dir) { await mkdir(dir, { recursive: true }); }
export async function readJsonIfExists(target) { try { return JSON.parse(await readFile(target, "utf8")); } catch { return null; } }
export async function writeJson(target, payload) { await ensureDir(path.dirname(target)); await writeFile(target, `${JSON.stringify(payload, null, 2)}\n`, "utf8"); }
export async function listFiles(dir) { try { return (await readdir(dir, { withFileTypes: true })).filter((e) => e.isFile() && e.name !== ".gitkeep").map((e) => e.name); } catch { return []; } }
export function getNowIso() { return new Date().toISOString(); }
export function classifyItem(files, requiredFiles) {
  if (files.length === 0) return "not_received";
  const missing = requiredFiles.filter((file) => !files.includes(file));
  if (missing.length === requiredFiles.length) return "received_but_invalid";
  if (missing.length > 0) return "received_partial";
  return "received_and_verified";
}
export function summarizeStatuses(items) {
  return items.reduce((acc, item) => {
    acc[item.status] = (acc[item.status] ?? 0) + 1;
    return acc;
  }, { not_received: 0, received_but_invalid: 0, received_partial: 0, received_and_verified: 0 });
}
export function runNode(scriptPath) {
  return new Promise((resolve) => {
    const child = spawn(process.execPath, [scriptPath], { cwd: ROOT, stdio: ["ignore", "pipe", "pipe"] });
    let stdout = ""; let stderr = "";
    child.stdout.on("data", (chunk) => (stdout += chunk.toString()));
    child.stderr.on("data", (chunk) => (stderr += chunk.toString()));
    child.on("close", (code) => resolve({ code, stdout, stderr }));
  });
}

