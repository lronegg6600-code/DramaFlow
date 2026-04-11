import { access, mkdir, readdir, readFile, writeFile } from "node:fs/promises";
import { constants } from "node:fs";
import path from "node:path";
import { spawn } from "node:child_process";

export const ROOT = path.resolve("Z:/Projects/DramaFlow");
export const EVIDENCE_DIR = path.join(ROOT, "release-evidence");
export const DOCS_DIR = path.join(ROOT, "docs");
export const PLATFORM_INTAKE_DIR = path.join(ROOT, "platform-intake");
export const STAGING_INPUT_DIR = path.join(PLATFORM_INTAKE_DIR, "received", "staging-inputs");
export const STAGING_INPUT_URLS_DIR = path.join(STAGING_INPUT_DIR, "urls");
export const STAGING_INPUT_MANIFESTS_DIR = path.join(STAGING_INPUT_DIR, "manifests");

export const STAGING_INPUT_ITEMS = [
  { id: "SI-001", key: "authBaseUrl", envVar: "DRAMAFLOW_AUTH_BASE_URL", title: "auth base URL", owner: "platform", blocksStaging: true },
  { id: "SI-002", key: "contentBaseUrl", envVar: "DRAMAFLOW_CONTENT_BASE_URL", title: "content base URL", owner: "platform", blocksStaging: true },
  { id: "SI-003", key: "feedBaseUrl", envVar: "DRAMAFLOW_FEED_BASE_URL", title: "feed base URL", owner: "platform", blocksStaging: true },
  { id: "SI-004", key: "progressBaseUrl", envVar: "DRAMAFLOW_PROGRESS_BASE_URL", title: "progress base URL", owner: "platform", blocksStaging: true },
  { id: "SI-005", key: "playbackBaseUrl", envVar: "DRAMAFLOW_PLAYBACK_BASE_URL", title: "playback base URL", owner: "platform", blocksStaging: true },
  { id: "SI-006", key: "entitlementBaseUrl", envVar: "DRAMAFLOW_ENTITLEMENT_BASE_URL", title: "entitlement base URL", owner: "platform", blocksStaging: true },
  { id: "SI-007", key: "billingBaseUrl", envVar: "DRAMAFLOW_BILLING_BASE_URL", title: "billing base URL", owner: "platform", blocksStaging: true },
];

export function getNowIso() {
  return new Date().toISOString();
}

export async function ensureDir(target) {
  await mkdir(target, { recursive: true });
}

export async function pathExists(target) {
  try {
    await access(target, constants.F_OK);
    return true;
  } catch {
    return false;
  }
}

export async function readJsonIfExists(target) {
  try {
    return JSON.parse(await readFile(target, "utf8"));
  } catch {
    return null;
  }
}

export async function writeJson(target, payload) {
  await ensureDir(path.dirname(target));
  await writeFile(target, `${JSON.stringify(payload, null, 2)}\n`, "utf8");
}

export async function writeText(target, content) {
  await ensureDir(path.dirname(target));
  await writeFile(target, content.endsWith("\n") ? content : `${content}\n`, "utf8");
}

export async function listNonGitkeepFiles(dir) {
  if (!(await pathExists(dir))) {
    return [];
  }
  const entries = await readdir(dir, { withFileTypes: true });
  return entries.filter((entry) => entry.isFile() && entry.name !== ".gitkeep").map((entry) => entry.name);
}

function normalizeScalar(value) {
  if (typeof value !== "string") return value;
  const trimmed = value.trim();
  if ((trimmed.startsWith("'") && trimmed.endsWith("'")) || (trimmed.startsWith("\"") && trimmed.endsWith("\""))) {
    return trimmed.slice(1, -1);
  }
  return trimmed;
}

export async function parseSimpleYamlOrJson(filePath) {
  const content = await readFile(filePath, "utf8");
  if (filePath.endsWith(".json")) {
    return JSON.parse(content);
  }
  const result = {};
  for (const rawLine of content.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#")) continue;
    const index = line.indexOf(":");
    if (index === -1) continue;
    const key = line.slice(0, index).trim();
    const value = normalizeScalar(line.slice(index + 1));
    result[key] = value;
  }
  return result;
}

export function isLikelyUrl(value) {
  if (!value || typeof value !== "string") return false;
  try {
    const parsed = new URL(value);
    return parsed.protocol === "http:" || parsed.protocol === "https:";
  } catch {
    return false;
  }
}

export async function loadStagingInputPayload() {
  const manifestFiles = await listNonGitkeepFiles(STAGING_INPUT_MANIFESTS_DIR);
  const urlFiles = await listNonGitkeepFiles(STAGING_INPUT_URLS_DIR);
  const candidateFiles = [
    ...manifestFiles.map((name) => path.join(STAGING_INPUT_MANIFESTS_DIR, name)),
    ...urlFiles.map((name) => path.join(STAGING_INPUT_URLS_DIR, name)),
  ];
  for (const filePath of candidateFiles) {
    const parsed = await parseSimpleYamlOrJson(filePath).catch(() => null);
    if (parsed && typeof parsed === "object") {
      return { filePath, payload: parsed, manifestFiles, urlFiles };
    }
  }
  return { filePath: null, payload: null, manifestFiles, urlFiles };
}

export async function buildStagingInputStatuses() {
  const { filePath, payload, manifestFiles, urlFiles } = await loadStagingInputPayload();
  const items = STAGING_INPUT_ITEMS.map((item) => {
    const value = payload?.[item.key] ?? null;
    const status = !payload
      ? "not_received"
      : !value
        ? "not_received"
        : isLikelyUrl(value)
          ? "verified"
          : "received_but_invalid";
    return {
      ...item,
      providedValue: value,
      status,
      sourceFile: filePath ? path.relative(ROOT, filePath) : null,
      rejectionReason:
        status === "received_but_invalid"
          ? `Invalid URL format for ${item.key}`
          : null,
    };
  });
  return {
    generatedAt: getNowIso(),
    sourceFile: filePath ? path.relative(ROOT, filePath) : null,
    manifestFiles,
    urlFiles,
    items,
  };
}

export function summarizeItemStatuses(items) {
  return items.reduce(
    (acc, item) => {
      acc[item.status] = (acc[item.status] ?? 0) + 1;
      return acc;
    },
    { not_received: 0, received_but_invalid: 0, verified: 0, closed: 0, escalated: 0 },
  );
}

export async function runNodeScript(scriptPath, extraEnv = {}) {
  return new Promise((resolve) => {
    const child = spawn(process.execPath, [scriptPath], {
      cwd: ROOT,
      env: { ...process.env, ...extraEnv },
      stdio: ["ignore", "pipe", "pipe"],
    });
    let stdout = "";
    let stderr = "";
    child.stdout.on("data", (chunk) => (stdout += chunk.toString()));
    child.stderr.on("data", (chunk) => (stderr += chunk.toString()));
    child.on("close", (code) => resolve({ code, stdout, stderr }));
  });
}
