import path from "node:path";
import { lookup } from "node:dns/promises";
import { spawn } from "node:child_process";
import {
  ROOT,
  EVIDENCE_DIR,
  PLATFORM_INTAKE_DIR,
  getNowIso,
  isLikelyUrl,
  listNonGitkeepFiles,
  parseSimpleYamlOrJson,
  pathExists,
  readJsonIfExists,
  runNodeScript,
  writeJson,
  writeText,
} from "./staging_input_common.mjs";

export { ROOT, EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson, writeText, runNodeScript };

export const REAL_EXTERNAL_INPUT_DIR = path.join(PLATFORM_INTAKE_DIR, "received", "staging-external-urls");
export const REAL_EXTERNAL_URLS_DIR = path.join(REAL_EXTERNAL_INPUT_DIR, "urls");
export const REAL_EXTERNAL_MANIFESTS_DIR = path.join(REAL_EXTERNAL_INPUT_DIR, "manifests");
export const REAL_EXTERNAL_RESPONSES_DIR = path.join(REAL_EXTERNAL_INPUT_DIR, "responses");
export const VERIFIED_ENV_FILE = path.join(ROOT, ".env.staging.mobile");
export const GH_PORTABLE = path.join(ROOT, "tools", "gh-portable", "bin", "gh.exe");
export const PLATFORM_ISSUE_NUMBER = 42;
export const PLATFORM_REPO = "lronegg6600-code/DramaFlow";
export const PLATFORM_OWNER_LOGIN = "lronegg6600-code";

async function resolveGhExecutable() {
  if (await pathExists(GH_PORTABLE)) {
    return GH_PORTABLE;
  }
  return "gh";
}

export const REAL_EXTERNAL_URL_ITEMS = [
  { id: "RE-001", key: "authBaseUrl", envVar: "DRAMAFLOW_AUTH_BASE_URL", title: "auth external base URL", owner: "platform" },
  { id: "RE-002", key: "contentBaseUrl", envVar: "DRAMAFLOW_CONTENT_BASE_URL", title: "content external base URL", owner: "platform" },
  { id: "RE-003", key: "feedBaseUrl", envVar: "DRAMAFLOW_FEED_BASE_URL", title: "feed external base URL", owner: "platform" },
  { id: "RE-004", key: "progressBaseUrl", envVar: "DRAMAFLOW_PROGRESS_BASE_URL", title: "progress external base URL", owner: "platform" },
  { id: "RE-005", key: "playbackBaseUrl", envVar: "DRAMAFLOW_PLAYBACK_BASE_URL", title: "playback external base URL", owner: "platform" },
  { id: "RE-006", key: "entitlementBaseUrl", envVar: "DRAMAFLOW_ENTITLEMENT_BASE_URL", title: "entitlement external base URL", owner: "platform" },
  { id: "RE-007", key: "billingBaseUrl", envVar: "DRAMAFLOW_BILLING_BASE_URL", title: "billing external base URL", owner: "platform" },
];

export function isCandidateFile(name) {
  return name.includes("candidate");
}

async function resolveUrlHost(value) {
  if (!isLikelyUrl(value)) {
    return { ok: false, reason: "Invalid URL format" };
  }
  try {
    const { hostname } = new URL(value);
    await lookup(hostname);
    return { ok: true, hostname };
  } catch {
    return { ok: false, reason: `Hostname did not resolve for ${value}` };
  }
}

export async function checkDnsResolution(value) {
  if (!isLikelyUrl(value)) {
    return { ok: false, reason: "Invalid URL format" };
  }
  try {
    const parsed = new URL(value);
    await lookup(parsed.hostname);
    return { ok: true, hostname: parsed.hostname };
  } catch {
    return { ok: false, reason: `Hostname did not resolve for ${value}` };
  }
}

export function checkPathPresence(value) {
  if (!isLikelyUrl(value)) {
    return { ok: false, reason: "Invalid URL format" };
  }
  const parsed = new URL(value);
  if (!parsed.pathname || parsed.pathname === "/") {
    return { ok: false, reason: `Missing service path for ${value}` };
  }
  return { ok: true, path: parsed.pathname };
}

export async function checkBasicReachability(value) {
  if (!isLikelyUrl(value)) {
    return { ok: false, reason: "Invalid URL format" };
  }
  try {
    const response = await fetch(value, { method: "HEAD", redirect: "manual" });
    return {
      ok: true,
      status: response.status,
      reason: `HTTP ${response.status}`,
    };
  } catch (error) {
    return {
      ok: false,
      reason: error?.message || `Reachability check failed for ${value}`,
    };
  }
}

async function listRealFiles(dir) {
  const files = await listNonGitkeepFiles(dir);
  return files.filter((name) => !isCandidateFile(name));
}

export async function loadRealExternalPayload() {
  const responseFiles = await listRealFiles(REAL_EXTERNAL_RESPONSES_DIR);
  const manifestFiles = await listRealFiles(REAL_EXTERNAL_MANIFESTS_DIR);
  const urlFiles = await listRealFiles(REAL_EXTERNAL_URLS_DIR);
  const files = [
    ...responseFiles.map((name) => path.join(REAL_EXTERNAL_RESPONSES_DIR, name)),
    ...manifestFiles.map((name) => path.join(REAL_EXTERNAL_MANIFESTS_DIR, name)),
    ...urlFiles.map((name) => path.join(REAL_EXTERNAL_URLS_DIR, name)),
  ];
  for (const filePath of files) {
    const parsed = await parseSimpleYamlOrJson(filePath).catch(() => null);
    if (parsed && typeof parsed === "object") {
      return { filePath, payload: parsed, responseFiles, manifestFiles, urlFiles };
    }
  }
  return { filePath: null, payload: null, responseFiles, manifestFiles, urlFiles };
}

export async function buildRealExternalStatuses() {
  const { filePath, payload, responseFiles, manifestFiles, urlFiles } = await loadRealExternalPayload();
  const items = [];
  for (const item of REAL_EXTERNAL_URL_ITEMS) {
    const value = payload?.[item.key] ?? null;
    let status = "real_url_not_received";
    let rejectionReason = null;
    if (payload && value) {
      const resolution = await resolveUrlHost(value);
      status = resolution.ok ? "verified" : "real_url_received_but_invalid";
      rejectionReason = resolution.ok ? null : resolution.reason;
    }
    items.push({
      ...item,
      providedValue: value,
      status,
      sourceFile: filePath ? path.relative(ROOT, filePath) : null,
      rejectionReason,
    });
  }
  return {
    generatedAt: getNowIso(),
    sourceFile: filePath ? path.relative(ROOT, filePath) : null,
    responseFiles,
    manifestFiles,
    urlFiles,
    items,
  };
}

export function summarizeRealStatuses(items) {
  return items.reduce(
    (acc, item) => {
      acc[item.status] = (acc[item.status] ?? 0) + 1;
      return acc;
    },
    { candidate_received_but_invalid: 0, real_url_not_received: 0, real_url_received_but_invalid: 0, verified: 0, closed: 0, escalated: 0 },
  );
}

export async function exportVerifiedEnv(items) {
  const allVerified = items.length === REAL_EXTERNAL_URL_ITEMS.length && items.every((item) => item.status === "verified");
  if (!allVerified) {
    return { exported: false, envFile: null };
  }
  const content = items.map((item) => `${item.envVar}=${item.providedValue}`).join("\n");
  await writeText(VERIFIED_ENV_FILE, content);
  return { exported: true, envFile: path.relative(ROOT, VERIFIED_ENV_FILE) };
}

export async function rerunWithVerifiedExternalUrls(items) {
  const allVerified = items.length === REAL_EXTERNAL_URL_ITEMS.length && items.every((item) => item.status === "verified");
  const scripts = [
    "mobile_backend_contract_smoke.mjs",
    "mobile_auth_feed_detail_flow.mjs",
    "mobile_playback_session_flow.mjs",
    "mobile_billing_entitlement_flow.mjs",
    "mobile_revoke_restore_flow.mjs",
  ];
  const results = [];
  if (allVerified) {
    const env = Object.fromEntries(items.map((item) => [item.envVar, item.providedValue]));
    for (const script of scripts) {
      results.push(await runNodeScript(path.join(ROOT, "backend", "tests", "integration", script), env));
    }
  }
  return {
    rerunExecuted: allVerified,
    scripts,
    results,
    reason: allVerified ? "Verified external staging URLs present; mobile rerun executed." : "Skipped due to unresolved external staging URLs.",
  };
}

export async function commentPlatformIssue(bodyFile) {
  const ghExec = await resolveGhExecutable();
  return new Promise((resolve) => {
    const child = spawn(ghExec, ["issue", "comment", String(PLATFORM_ISSUE_NUMBER), "--repo", PLATFORM_REPO, "--body-file", bodyFile], {
      cwd: ROOT,
      stdio: ["ignore", "pipe", "pipe"],
    });
    let stdout = "";
    let stderr = "";
    child.stdout.on("data", (chunk) => (stdout += chunk.toString()));
    child.stderr.on("data", (chunk) => (stderr += chunk.toString()));
    child.on("error", (error) =>
      resolve({
        ok: false,
        reason: error?.message || "Failed to execute gh issue comment.",
        code: -1,
        stdout: stdout.trim(),
        stderr: stderr.trim(),
      }),
    );
    child.on("close", (code) => resolve({ ok: code === 0, code, stdout: stdout.trim(), stderr: stderr.trim() }));
  });
}

export async function fetchPlatformIssueComments() {
  const ghExec = await resolveGhExecutable();
  return new Promise((resolve) => {
    const child = spawn(
      ghExec,
      ["issue", "view", String(PLATFORM_ISSUE_NUMBER), "--repo", PLATFORM_REPO, "--json", "comments"],
      { cwd: ROOT, stdio: ["ignore", "pipe", "pipe"] },
    );
    let stdout = "";
    let stderr = "";
    child.stdout.on("data", (chunk) => (stdout += chunk.toString()));
    child.stderr.on("data", (chunk) => (stderr += chunk.toString()));
    child.on("error", (error) => {
      resolve({ ok: false, reason: error?.message || "Failed to execute gh issue view.", comments: [] });
    });
    child.on("close", (code) => {
      if (code !== 0) {
        resolve({ ok: false, reason: stderr.trim() || "Failed to fetch issue comments.", comments: [] });
        return;
      }
      try {
        const parsed = JSON.parse(stdout);
        resolve({ ok: true, comments: parsed.comments || [] });
      } catch {
        resolve({ ok: false, reason: "Failed to parse issue comments.", comments: [] });
      }
    });
  });
}
