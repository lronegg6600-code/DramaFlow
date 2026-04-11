import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";

const THIS_FILE = fileURLToPath(import.meta.url);
const ROOT = path.resolve(path.dirname(THIS_FILE), "..");
const RESP_DIR = path.join(
  ROOT,
  "platform-intake",
  "received",
  "staging-external-urls",
  "responses",
);
const RESP_FILE = path.join(RESP_DIR, "staging-external-urls.yaml");
const EVIDENCE = path.join(ROOT, "release-evidence", "external-url-manifest-generation.json");

const defaultHost = process.env.STAGING_GATEWAY_HOST || "api.staging.dramaflow.example";
const defaultScheme = process.env.STAGING_GATEWAY_SCHEME || "https";

const ROUTES = {
  authBaseUrl: "/auth",
  contentBaseUrl: "/content",
  feedBaseUrl: "/feed",
  progressBaseUrl: "/progress",
  playbackBaseUrl: "/playback",
  entitlementBaseUrl: "/entitlement",
  billingBaseUrl: "/billing",
};

function run(command, args = []) {
  const r = spawnSync(command, args, { cwd: ROOT, encoding: "utf8" });
  return {
    ok: r.status === 0,
    status: r.status ?? -1,
    stdout: (r.stdout || "").trim(),
    stderr: (r.stderr || "").trim(),
    error: r.error ? String(r.error.message || r.error) : null,
  };
}

function writeJson(filePath, payload) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, "utf8");
}

function buildUrl(scheme, host, route) {
  const normalizedRoute = route.startsWith("/") ? route : `/${route}`;
  return `${scheme}://${host}${normalizedRoute}`;
}

const host = defaultHost.trim();
const scheme = defaultScheme.trim();
const generatedAt = new Date().toISOString();

const urls = Object.fromEntries(
  Object.entries(ROUTES).map(([key, route]) => [key, buildUrl(scheme, host, route)]),
);

fs.mkdirSync(RESP_DIR, { recursive: true });
const yaml = `${Object.entries(urls)
  .map(([k, v]) => `${k}: ${v}`)
  .join("\n")}\n`;
fs.writeFileSync(RESP_FILE, yaml, "utf8");

const validate = run(process.execPath, [path.join(ROOT, "tools", "validate_real_external_urls.mjs")]);
const summarize = run(process.execPath, [path.join(ROOT, "tools", "summarize_real_external_url_burndown.mjs")]);

const payload = {
  generatedAt,
  host,
  scheme,
  responseFile: path.relative(ROOT, RESP_FILE),
  urls,
  validate,
  summarize,
  note:
    "This script generates the 7-key manifest from one gateway host. Validation must still pass (7/7 verified) before staging integration.",
};

writeJson(EVIDENCE, payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(validate.ok && summarize.ok ? 0 : 1);

