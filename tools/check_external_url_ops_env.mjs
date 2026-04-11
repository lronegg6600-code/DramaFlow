import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";

const THIS_FILE = fileURLToPath(import.meta.url);
const ROOT = path.resolve(path.dirname(THIS_FILE), "..");
const EVIDENCE_DIR = path.join(ROOT, "release-evidence");
const OUTPUT = path.join(EVIDENCE_DIR, "external-url-ops-env-check.json");

function run(command, args = [], options = {}) {
  const result = spawnSync(command, args, {
    cwd: ROOT,
    encoding: "utf8",
    ...options,
  });
  return {
    ok: result.status === 0,
    status: result.status ?? -1,
    stdout: (result.stdout || "").trim(),
    stderr: (result.stderr || "").trim(),
    error: result.error ? String(result.error.message || result.error) : null,
  };
}

function safeWriteJson(filePath, payload) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, "utf8");
}

function locateGh() {
  const candidates = [
    process.env.GH_BIN,
    "gh",
    "Z:\\gh\\bin\\gh.exe",
    "Z:\\GitHubCLI\\bin\\gh.exe",
    "C:\\Program Files\\GitHub CLI\\gh.exe",
    "C:\\Program Files (x86)\\GitHub CLI\\gh.exe",
  ].filter(Boolean);

  const deduped = [...new Set(candidates)];
  for (const candidate of deduped) {
    if (candidate !== "gh" && !fs.existsSync(candidate)) {
      continue;
    }
    const probe = run(candidate, ["--version"]);
    if (probe.ok) {
      return {
        available: true,
        command: candidate,
        version: probe.stdout.split(/\r?\n/)[0] || "unknown",
        probe,
      };
    }
  }
  return {
    available: false,
    command: null,
    version: null,
    probe: null,
  };
}

function checkToken() {
  const token = process.env.GH_TOKEN || process.env.GITHUB_TOKEN || "";
  return {
    available: token.trim().length > 0,
    source: process.env.GH_TOKEN ? "GH_TOKEN" : process.env.GITHUB_TOKEN ? "GITHUB_TOKEN" : null,
  };
}

function checkGhAuth(ghCommand) {
  if (!ghCommand) {
    return { authenticated: false, probe: null };
  }
  const probe = run(ghCommand, ["auth", "status", "--hostname", "github.com"]);
  return {
    authenticated: probe.ok,
    probe,
  };
}

function checkRepoRemote() {
  const remoteProbe = run("git", ["-C", ROOT, "remote", "get-url", "origin"]);
  const remote = remoteProbe.ok ? remoteProbe.stdout : null;
  const validPattern = /(github\.com[:/])lronegg6600-code\/DramaFlow(\.git)?$/i;
  return {
    available: remoteProbe.ok,
    remote,
    valid: remoteProbe.ok ? validPattern.test(remote || "") : false,
    probe: remoteProbe,
  };
}

function checkEvidenceWritable() {
  try {
    fs.mkdirSync(EVIDENCE_DIR, { recursive: true });
    const marker = path.join(EVIDENCE_DIR, `.env-check-write-test-${Date.now()}.tmp`);
    fs.writeFileSync(marker, "ok\n", "utf8");
    fs.unlinkSync(marker);
    return { writable: true, error: null };
  } catch (error) {
    return { writable: false, error: String(error.message || error) };
  }
}

function decideOverallStatus(checks) {
  if (!checks.gh.available) return "blocked_by_missing_gh";
  if (!checks.token.available) return "blocked_by_missing_token";
  if (!checks.ghAuth.authenticated) return "blocked_by_gh_not_authenticated";
  if (!checks.remote.valid) return "blocked_by_invalid_repo_remote";
  if (!checks.evidence.writable) return "blocked_by_evidence_dir_unwritable";
  return "ready";
}

export function runExternalUrlOpsEnvCheck() {
  const generatedAt = new Date().toISOString();
  const gh = locateGh();
  const token = checkToken();
  const ghAuth = checkGhAuth(gh.command);
  const remote = checkRepoRemote();
  const evidence = checkEvidenceWritable();

  const checks = { gh, token, ghAuth, remote, evidence };
  const overallStatus = decideOverallStatus(checks);
  const payload = {
    generatedAt,
    overallStatus,
    ready: overallStatus === "ready",
    checks: {
      gh: {
        available: gh.available,
        command: gh.command,
        version: gh.version,
        error: gh.probe?.error ?? null,
      },
      token: token,
      ghAuth: {
        authenticated: ghAuth.authenticated,
        statusCode: ghAuth.probe?.status ?? null,
        stderr: ghAuth.probe?.stderr ?? null,
      },
      repoRemote: {
        available: remote.available,
        remote: remote.remote,
        valid: remote.valid,
        stderr: remote.probe?.stderr ?? null,
      },
      evidenceDir: {
        path: EVIDENCE_DIR,
        writable: evidence.writable,
        error: evidence.error,
      },
    },
    blockers: Object.entries({
      blocked_by_missing_gh: !gh.available,
      blocked_by_missing_token: gh.available && !token.available,
      blocked_by_gh_not_authenticated: gh.available && token.available && !ghAuth.authenticated,
      blocked_by_invalid_repo_remote: gh.available && token.available && ghAuth.authenticated && !remote.valid,
      blocked_by_evidence_dir_unwritable:
        gh.available && token.available && ghAuth.authenticated && remote.valid && !evidence.writable,
    })
      .filter(([, active]) => active)
      .map(([id]) => id),
  };

  safeWriteJson(OUTPUT, payload);

  console.log("[external-url-env-check] --------------------------------");
  console.log(`gh: ${gh.available ? `ready (${gh.command})` : "missing"}`);
  console.log(`token: ${token.available ? `ready (${token.source})` : "missing"}`);
  console.log(`gh auth: ${ghAuth.authenticated ? "ready" : "not authenticated"}`);
  console.log(`repo remote: ${remote.valid ? "ready" : "invalid"}`);
  console.log(`release-evidence writable: ${evidence.writable ? "ready" : "blocked"}`);
  console.log(`overall: ${payload.overallStatus}`);
  console.log(`json: ${path.relative(ROOT, OUTPUT)}`);
  console.log(JSON.stringify(payload, null, 2));

  return payload;
}

if (process.argv[1] && path.resolve(process.argv[1]) === THIS_FILE) {
  const payload = runExternalUrlOpsEnvCheck();
  process.exit(payload.ready ? 0 : 1);
}
