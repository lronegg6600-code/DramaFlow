import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";

import { runExternalUrlOpsEnvCheck } from "./check_external_url_ops_env.mjs";

const THIS_FILE = fileURLToPath(import.meta.url);
const ROOT = path.resolve(path.dirname(THIS_FILE), "..");
const EVIDENCE_DIR = path.join(ROOT, "release-evidence");
const OUTPUT = path.join(EVIDENCE_DIR, "external-url-unblock-cycle-summary.json");

function safeReadJson(filePath, fallback = null) {
  try {
    if (!fs.existsSync(filePath)) return fallback;
    return JSON.parse(fs.readFileSync(filePath, "utf8"));
  } catch {
    return fallback;
  }
}

function safeWriteJson(filePath, payload) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, "utf8");
}

function runNodeScript(scriptName) {
  const scriptPath = path.join(ROOT, "tools", scriptName);
  const result = spawnSync(process.execPath, [scriptPath], {
    cwd: ROOT,
    encoding: "utf8",
  });
  return {
    script: `node tools/${scriptName}`,
    ok: result.status === 0,
    status: result.status ?? -1,
    stdout: (result.stdout || "").trim(),
    stderr: (result.stderr || "").trim(),
    error: result.error ? String(result.error.message || result.error) : null,
  };
}

function deriveCycleStatus(envStatus, steps) {
  if (envStatus !== "ready") return envStatus;
  const validation = safeReadJson(path.join(EVIDENCE_DIR, "staging-external-url-second-validation.json"), null);
  if (validation?.statuses?.real_url_not_received > 0) return "blocked_by_missing_external_urls";
  if (validation?.statuses?.real_url_received_but_invalid > 0) return "blocked_by_invalid_external_urls";
  if (!steps.every((step) => step.ok)) return "blocked_by_cycle_execution_error";
  if (validation?.allVerified) return "ready_for_staging_integration";
  return "blocked_by_unresolved_external_urls";
}

function buildNextActions(cycleStatus) {
  const actions = {
    blocked_by_missing_gh: [
      "Install GitHub CLI and ensure `gh` is available in PATH or set GH_BIN.",
      "Re-run: node tools/check_external_url_ops_env.mjs",
    ],
    blocked_by_missing_token: [
      "Set GH_TOKEN or GITHUB_TOKEN for this shell.",
      "Re-run: node tools/check_external_url_ops_env.mjs",
    ],
    blocked_by_gh_not_authenticated: [
      "Run `gh auth login` and ensure `gh auth status` returns success.",
      "Re-run: node tools/check_external_url_ops_env.mjs",
    ],
    blocked_by_invalid_repo_remote: [
      "Fix origin remote to `github.com/lronegg6600-code/DramaFlow(.git)`.",
      "Re-run: node tools/check_external_url_ops_env.mjs",
    ],
    blocked_by_evidence_dir_unwritable: [
      "Fix write permission on `release-evidence/`.",
      "Re-run: node tools/check_external_url_ops_env.mjs",
    ],
    blocked_by_missing_external_urls: [
      "Platform must submit all 7 real external staging URLs.",
      "Run the cycle again after intake file is updated.",
    ],
    blocked_by_invalid_external_urls: [
      "Reject invalid URLs and request corrected resolvable endpoints.",
      "Run the cycle again after corrected input delivery.",
    ],
    blocked_by_cycle_execution_error: [
      "Inspect step stderr in this summary file and fix script/runtime issue.",
      "Re-run cycle once script failures are resolved.",
    ],
    blocked_by_unresolved_external_urls: [
      "Continue follow-up until validation reaches 7/7 verified.",
      "Re-run cycle after new platform reply/input.",
    ],
    ready_for_staging_integration: [
      "Run Android × backend staging integration flows immediately.",
      "Update integration evidence and readiness decision docs.",
    ],
  };
  return actions[cycleStatus] || ["Inspect cycle summary and proceed with targeted fix."];
}

function runCycle() {
  const generatedAt = new Date().toISOString();
  const env = runExternalUrlOpsEnvCheck();

  if (!env.ready) {
    const payload = {
      generatedAt,
      mode: "preflight_blocked",
      cycleStatus: env.overallStatus,
      envCheck: {
        status: env.overallStatus,
        json: "release-evidence/external-url-ops-env-check.json",
      },
      steps: [],
      summary: {
        message: "Environment is blocked. External URL cycle was not executed.",
        nextActions: buildNextActions(env.overallStatus),
      },
    };
    safeWriteJson(OUTPUT, payload);
    console.log(JSON.stringify(payload, null, 2));
    return { payload, exitCode: 1 };
  }

  const steps = [
    runNodeScript("fetch_real_external_url_replies.mjs"),
    runNodeScript("validate_real_external_urls.mjs"),
    runNodeScript("summarize_real_external_url_burndown.mjs"),
  ];

  const cycleStatus = deriveCycleStatus(env.overallStatus, steps);
  const burndown = safeReadJson(path.join(EVIDENCE_DIR, "staging-external-url-second-burndown.json"), null);
  const validation = safeReadJson(path.join(EVIDENCE_DIR, "staging-external-url-second-validation.json"), null);
  const replies = safeReadJson(path.join(EVIDENCE_DIR, "staging-real-external-url-reply-log.json"), null);

  const payload = {
    generatedAt,
    mode: "executed",
    cycleStatus,
    envCheck: {
      status: env.overallStatus,
      json: "release-evidence/external-url-ops-env-check.json",
    },
    steps,
    keyMetrics: {
      replyFetchSucceeded: replies?.fetchSucceeded ?? false,
      platformReplyCount: replies?.platformReplyCount ?? 0,
      urlsReceived: burndown?.realExternalUrlReceivedCount ?? 0,
      urlsVerified: validation?.statuses?.verified ?? 0,
      urlsMissing: validation?.statuses?.real_url_not_received ?? 0,
      urlsInvalid: validation?.statuses?.real_url_received_but_invalid ?? 0,
      rerunExecuted: burndown?.rerunExecuted ?? false,
    },
    summary: {
      message:
        cycleStatus === "ready_for_staging_integration"
          ? "All external URL gates are satisfied. Staging integration can start."
          : "Cycle executed, but external URL gate is not fully satisfied yet.",
      nextActions: buildNextActions(cycleStatus),
    },
  };

  safeWriteJson(OUTPUT, payload);
  console.log(JSON.stringify(payload, null, 2));
  return { payload, exitCode: cycleStatus === "ready_for_staging_integration" ? 0 : 1 };
}

if (process.argv[1] && path.resolve(process.argv[1]) === THIS_FILE) {
  const { exitCode } = runCycle();
  process.exit(exitCode);
}

