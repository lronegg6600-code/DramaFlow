import path from "node:path";
import net from "node:net";
import { spawn } from "node:child_process";
import { ROOT, EVIDENCE_DIR, DOCS_DIR, ensureDir, getNowIso, writeJson, writeText, readJsonIfExists } from "./staging_input_common.mjs";

export { ROOT, EVIDENCE_DIR, DOCS_DIR, getNowIso, writeJson, writeText, readJsonIfExists };

export const COMPOSE_FILE = path.join(ROOT, "backend", "deployments", "docker-compose", "docker-compose.yml");
export const LOCAL_DEBUG_ENV_FILE = path.join(ROOT, ".env.local.mobile.debug");
export const LOCAL_DEBUG_URLS = {
  authBaseUrl: "http://10.0.2.2:8081/",
  contentBaseUrl: "http://10.0.2.2:8082/",
  feedBaseUrl: "http://10.0.2.2:8083/",
  progressBaseUrl: "http://10.0.2.2:8084/",
  playbackBaseUrl: "http://10.0.2.2:8085/",
  entitlementBaseUrl: "http://10.0.2.2:8086/",
  billingBaseUrl: "http://10.0.2.2:8087/",
};
export const HOST_DEBUG_URLS = {
  authBaseUrl: "http://127.0.0.1:8081/",
  contentBaseUrl: "http://127.0.0.1:8082/",
  feedBaseUrl: "http://127.0.0.1:8083/",
  progressBaseUrl: "http://127.0.0.1:8084/",
  playbackBaseUrl: "http://127.0.0.1:8085/",
  entitlementBaseUrl: "http://127.0.0.1:8086/",
  billingBaseUrl: "http://127.0.0.1:8087/",
};
export const SERVICE_PORTS = [
  { name: "auth-service", port: 8081 },
  { name: "content-service", port: 8082 },
  { name: "feed-service", port: 8083 },
  { name: "progress-service", port: 8084 },
  { name: "playback-service", port: 8085 },
  { name: "entitlement-service", port: 8086 },
  { name: "billing-service", port: 8087 },
];

export async function runCommand(command, args = [], { timeoutMs = 15000, cwd = ROOT } = {}) {
  return new Promise((resolve) => {
    const child = spawn(command, args, { cwd, stdio: ["ignore", "pipe", "pipe"] });
    let stdout = "";
    let stderr = "";
    let timedOut = false;
    const timer = setTimeout(() => {
      timedOut = true;
      child.kill();
    }, timeoutMs);
    child.stdout.on("data", (chunk) => (stdout += chunk.toString()));
    child.stderr.on("data", (chunk) => (stderr += chunk.toString()));
    child.on("close", (code) => {
      clearTimeout(timer);
      resolve({ command, args, code, stdout: stdout.trim(), stderr: stderr.trim(), timedOut });
    });
  });
}

export async function runPowershell(script, { timeoutMs = 15000 } = {}) {
  return runCommand("C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe", ["-Command", script], { timeoutMs });
}

export async function waitForDocker({ attempts = 6, sleepSeconds = 5 } = {}) {
  const attemptsLog = [];
  for (let index = 0; index < attempts; index += 1) {
    const info = await runCommand("docker", ["info"], { timeoutMs: 10000 });
    attemptsLog.push({ attempt: index + 1, info });
    if (info.code === 0 && !info.timedOut) {
      return { ready: true, attempts: attemptsLog };
    }
    if (index < attempts - 1) {
      await runPowershell(`Start-Sleep -Seconds ${sleepSeconds}`, { timeoutMs: (sleepSeconds + 2) * 1000 });
    }
  }
  return { ready: false, attempts: attemptsLog };
}

export async function checkPort(port, host = "127.0.0.1", timeoutMs = 2500) {
  return new Promise((resolve) => {
    const socket = new net.Socket();
    let settled = false;
    const finish = (payload) => {
      if (!settled) {
        settled = true;
        socket.destroy();
        resolve(payload);
      }
    };
    socket.setTimeout(timeoutMs);
    socket.once("connect", () => finish({ ok: true }));
    socket.once("timeout", () => finish({ ok: false, reason: "timeout" }));
    socket.once("error", (error) => finish({ ok: false, reason: error.message }));
    socket.connect(port, host);
  });
}

export async function checkHealth(url) {
  try {
    const response = await fetch(url, { method: "GET" });
    return { ok: response.ok, status: response.status };
  } catch (error) {
    return { ok: false, reason: error?.message || "request_failed" };
  }
}

export async function writeLocalDebugEnvFile() {
  const lines = [
    `DRAMAFLOW_AUTH_BASE_URL=${LOCAL_DEBUG_URLS.authBaseUrl}`,
    `DRAMAFLOW_CONTENT_BASE_URL=${LOCAL_DEBUG_URLS.contentBaseUrl}`,
    `DRAMAFLOW_FEED_BASE_URL=${LOCAL_DEBUG_URLS.feedBaseUrl}`,
    `DRAMAFLOW_PROGRESS_BASE_URL=${LOCAL_DEBUG_URLS.progressBaseUrl}`,
    `DRAMAFLOW_PLAYBACK_BASE_URL=${LOCAL_DEBUG_URLS.playbackBaseUrl}`,
    `DRAMAFLOW_ENTITLEMENT_BASE_URL=${LOCAL_DEBUG_URLS.entitlementBaseUrl}`,
    `DRAMAFLOW_BILLING_BASE_URL=${LOCAL_DEBUG_URLS.billingBaseUrl}`,
  ];
  await ensureDir(path.dirname(LOCAL_DEBUG_ENV_FILE));
  await writeText(LOCAL_DEBUG_ENV_FILE, lines.join("\n"));
  return path.relative(ROOT, LOCAL_DEBUG_ENV_FILE);
}

export async function runLocalMobileScripts() {
  const scripts = [
    "mobile_backend_contract_smoke.mjs",
    "mobile_auth_feed_detail_flow.mjs",
    "mobile_playback_session_flow.mjs",
    "mobile_billing_entitlement_flow.mjs",
    "mobile_revoke_restore_flow.mjs",
  ];
  const env = {
    ...process.env,
    DRAMAFLOW_AUTH_BASE_URL: HOST_DEBUG_URLS.authBaseUrl,
    DRAMAFLOW_CONTENT_BASE_URL: HOST_DEBUG_URLS.contentBaseUrl,
    DRAMAFLOW_FEED_BASE_URL: HOST_DEBUG_URLS.feedBaseUrl,
    DRAMAFLOW_PROGRESS_BASE_URL: HOST_DEBUG_URLS.progressBaseUrl,
    DRAMAFLOW_PLAYBACK_BASE_URL: HOST_DEBUG_URLS.playbackBaseUrl,
    DRAMAFLOW_ENTITLEMENT_BASE_URL: HOST_DEBUG_URLS.entitlementBaseUrl,
    DRAMAFLOW_BILLING_BASE_URL: HOST_DEBUG_URLS.billingBaseUrl,
  };
  const results = [];
  for (const script of scripts) {
    const result = await new Promise((resolve) => {
      const child = spawn(process.execPath, [path.join(ROOT, "backend", "tests", "integration", script)], {
        cwd: ROOT,
        env,
        stdio: ["ignore", "pipe", "pipe"],
      });
      let stdout = "";
      let stderr = "";
      let timedOut = false;
      const timer = setTimeout(() => {
        timedOut = true;
        child.kill();
      }, 30000);
      child.stdout.on("data", (chunk) => (stdout += chunk.toString()));
      child.stderr.on("data", (chunk) => (stderr += chunk.toString()));
      child.on("close", (code) => {
        clearTimeout(timer);
        resolve({ script, code, stdout: stdout.trim(), stderr: stderr.trim(), timedOut });
      });
    });
    results.push(result);
  }
  return { scripts, results };
}
