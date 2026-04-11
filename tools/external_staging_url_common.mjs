import path from "node:path";
import { lookup } from "node:dns/promises";
import {
  ROOT,
  EVIDENCE_DIR,
  PLATFORM_INTAKE_DIR,
  ensureDir,
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

export { ROOT, EVIDENCE_DIR, ensureDir, getNowIso, readJsonIfExists, writeJson, writeText };

export const EXTERNAL_INPUT_DIR = path.join(PLATFORM_INTAKE_DIR, "received", "staging-external-urls");
export const EXTERNAL_URLS_DIR = path.join(EXTERNAL_INPUT_DIR, "urls");
export const EXTERNAL_MANIFESTS_DIR = path.join(EXTERNAL_INPUT_DIR, "manifests");

export const EXTERNAL_URL_ITEMS = [
  { id: "SE-001", key: "authBaseUrl", envVar: "DRAMAFLOW_AUTH_BASE_URL", title: "auth external base URL", owner: "platform" },
  { id: "SE-002", key: "contentBaseUrl", envVar: "DRAMAFLOW_CONTENT_BASE_URL", title: "content external base URL", owner: "platform" },
  { id: "SE-003", key: "feedBaseUrl", envVar: "DRAMAFLOW_FEED_BASE_URL", title: "feed external base URL", owner: "platform" },
  { id: "SE-004", key: "progressBaseUrl", envVar: "DRAMAFLOW_PROGRESS_BASE_URL", title: "progress external base URL", owner: "platform" },
  { id: "SE-005", key: "playbackBaseUrl", envVar: "DRAMAFLOW_PLAYBACK_BASE_URL", title: "playback external base URL", owner: "platform" },
  { id: "SE-006", key: "entitlementBaseUrl", envVar: "DRAMAFLOW_ENTITLEMENT_BASE_URL", title: "entitlement external base URL", owner: "platform" },
  { id: "SE-007", key: "billingBaseUrl", envVar: "DRAMAFLOW_BILLING_BASE_URL", title: "billing external base URL", owner: "platform" },
];

async function resolveUrlHost(value) {
  if (!isLikelyUrl(value)) {
    return { ok: false, reason: "Invalid external URL format" };
  }
  try {
    const { hostname } = new URL(value);
    await lookup(hostname);
    return { ok: true, hostname };
  } catch (error) {
    return { ok: false, reason: `Hostname did not resolve for ${value}` };
  }
}

export const INTERNAL_SERVICE_MAP = [
  {
    key: "authBaseUrl",
    url: "http://auth-service:8081",
    internal_only: true,
    verified_from_repo: true,
    android_external_access: "not_verified",
    source_files: [
      "backend/deployments/k8s/base/auth-service.yaml",
    ],
  },
  {
    key: "contentBaseUrl",
    url: "http://content-service:8082",
    internal_only: true,
    verified_from_repo: true,
    android_external_access: "not_verified",
    source_files: [
      "backend/deployments/helm/values/staging.values.example.yaml",
      "backend/deployments/k8s/base/content-service.yaml",
    ],
  },
  {
    key: "feedBaseUrl",
    url: "http://feed-service:8083",
    internal_only: true,
    verified_from_repo: true,
    android_external_access: "not_verified",
    source_files: [
      "backend/deployments/helm/values/staging.values.example.yaml",
      "backend/deployments/k8s/base/feed-service.yaml",
    ],
  },
  {
    key: "progressBaseUrl",
    url: "http://progress-service:8084",
    internal_only: true,
    verified_from_repo: true,
    android_external_access: "not_verified",
    source_files: [
      "backend/deployments/k8s/base/progress-service.yaml",
    ],
  },
  {
    key: "playbackBaseUrl",
    url: "http://playback-service:8085",
    internal_only: true,
    verified_from_repo: true,
    android_external_access: "not_verified",
    source_files: [
      "backend/deployments/helm/values/staging.values.example.yaml",
      "backend/deployments/k8s/base/playback-service.yaml",
    ],
  },
  {
    key: "entitlementBaseUrl",
    url: "http://entitlement-service:8086",
    internal_only: true,
    verified_from_repo: true,
    android_external_access: "not_verified",
    source_files: [
      "backend/deployments/helm/values/staging.values.example.yaml",
      "backend/deployments/k8s/base/entitlement-service.yaml",
    ],
  },
  {
    key: "billingBaseUrl",
    url: "http://billing-service:8087",
    internal_only: true,
    verified_from_repo: true,
    android_external_access: "not_verified",
    source_files: [
      "backend/deployments/helm/values/staging.values.example.yaml",
      "backend/deployments/k8s/base/billing-service.yaml",
    ],
  },
];

export async function loadExternalUrlPayload() {
  const manifestFiles = await listNonGitkeepFiles(EXTERNAL_MANIFESTS_DIR);
  const urlFiles = await listNonGitkeepFiles(EXTERNAL_URLS_DIR);
  const candidateFiles = [
    ...manifestFiles.map((name) => path.join(EXTERNAL_MANIFESTS_DIR, name)),
    ...urlFiles.map((name) => path.join(EXTERNAL_URLS_DIR, name)),
  ];
  for (const filePath of candidateFiles) {
    const parsed = await parseSimpleYamlOrJson(filePath).catch(() => null);
    if (parsed && typeof parsed === "object") {
      return { filePath, payload: parsed, manifestFiles, urlFiles };
    }
  }
  return { filePath: null, payload: null, manifestFiles, urlFiles };
}

export async function buildExternalUrlStatuses() {
  const { filePath, payload, manifestFiles, urlFiles } = await loadExternalUrlPayload();
  const items = [];
  for (const item of EXTERNAL_URL_ITEMS) {
    const value = payload?.[item.key] ?? null;
    let status = "not_received";
    let rejectionReason = null;
    if (payload && value) {
      const resolution = await resolveUrlHost(value);
      status = resolution.ok ? "verified" : "received_but_invalid";
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
    manifestFiles,
    urlFiles,
    items,
  };
}

export function summarizeExternalStatuses(items) {
  return items.reduce(
    (acc, item) => {
      acc[item.status] = (acc[item.status] ?? 0) + 1;
      return acc;
    },
    { not_received: 0, received_but_invalid: 0, verified: 0, closed: 0, escalated: 0 },
  );
}

export async function exportAndroidEnvFromExternalUrls(items) {
  const allVerified = items.every((item) => item.status === "verified");
  const envFile = path.join(ROOT, ".env.staging.mobile");
  if (!allVerified) {
    return { exported: false, envFile: null };
  }
  const content = items.map((item) => `${item.envVar}=${item.providedValue}`).join("\n");
  await writeText(envFile, content);
  return { exported: true, envFile: path.relative(ROOT, envFile) };
}

export async function rerunMobileGateWithExternalUrls(items) {
  const allVerified = items.every((item) => item.status === "verified");
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
    reason: allVerified ? "External staging URLs verified; mobile integration rerun executed." : "Skipped due to incomplete external staging URLs.",
  };
}

export async function externalInputDirsExist() {
  return {
    urlsDirExists: await pathExists(EXTERNAL_URLS_DIR),
    manifestsDirExists: await pathExists(EXTERNAL_MANIFESTS_DIR),
  };
}
