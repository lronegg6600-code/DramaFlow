import { mkdir, readdir, readFile, writeFile, access } from "node:fs/promises";
import { constants } from "node:fs";
import path from "node:path";
import { lookup } from "node:dns/promises";

const ROOT = path.resolve(path.dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, "$1")), "../../..");
const EVIDENCE_DIR = path.join(ROOT, "release-evidence");

async function pathExists(target) {
  try {
    await access(target, constants.F_OK);
    return true;
  } catch {
    return false;
  }
}

async function validateResolvableUrl(value) {
  if (!value) {
    return { ok: false, reason: "missing" };
  }
  try {
    const parsed = new URL(value);
    await lookup(parsed.hostname);
    return { ok: true, hostname: parsed.hostname };
  } catch (error) {
    return { ok: false, reason: `unresolvable_or_invalid:${value}` };
  }
}

async function listFiles(dir, matcher, results = []) {
  if (!(await pathExists(dir))) {
    return results;
  }
  const entries = await readdir(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === "build" || entry.name === ".gradle" || entry.name === ".idea" || entry.name === ".kotlin") {
        continue;
      }
      await listFiles(fullPath, matcher, results);
    } else if (matcher(fullPath)) {
      results.push(fullPath);
    }
  }
  return results;
}

export async function getWorkspaceSnapshot() {
  const androidDir = path.join(ROOT, "android");
  const backendDir = path.join(ROOT, "backend");
  const docsDir = path.join(ROOT, "docs");
  const gitDir = path.join(ROOT, ".git");
  const backendServicesDir = path.join(backendDir, "services");
  const androidSourceFiles = await listFiles(
    androidDir,
    (fullPath) =>
      fullPath.endsWith(".kt") ||
      fullPath.endsWith(".kts") ||
      fullPath.endsWith(".properties") ||
      fullPath.endsWith("AndroidManifest.xml"),
  );
  const backendSourceFiles = await listFiles(
    backendDir,
    (fullPath) => fullPath.endsWith(".mjs") || fullPath.endsWith(".go") || fullPath.endsWith(".yaml") || fullPath.endsWith(".yml"),
  );

  const blockers = [];
  if (!(await pathExists(gitDir))) {
    blockers.push({
      id: "MB-ENV-001",
      title: "Git metadata is missing in current workspace",
      area: "environment/config",
      blockerForStaging: true,
      owner: "repo admin",
    });
  }
  if (!(await pathExists(androidDir)) || androidSourceFiles.length === 0) {
    blockers.push({
      id: "MB-ENV-002",
      title: "Android source tree is missing; only generated build outputs are present",
      area: "android",
      blockerForStaging: true,
      owner: "android owner",
    });
  }
  if (!(await pathExists(backendServicesDir))) {
    blockers.push({
      id: "MB-ENV-003",
      title: "Backend source and integration harness are missing from current workspace",
      area: "backend",
      blockerForStaging: true,
      owner: "backend owner",
    });
  }
  if (!(await pathExists(docsDir))) {
    blockers.push({
      id: "MB-ENV-004",
      title: "Release documentation tree is missing from current workspace",
      area: "environment/config",
      blockerForStaging: false,
      owner: "engineering",
    });
  }

  return {
    generatedAt: new Date().toISOString(),
    root: ROOT,
    androidDir,
    backendDir,
    backendServicesDir,
    docsDir,
    gitDir,
    androidSourceCount: androidSourceFiles.length,
    backendSourceCount: backendSourceFiles.length,
    androidSourceSample: androidSourceFiles.slice(0, 10).map((fullPath) => path.relative(ROOT, fullPath)),
    backendSourceSample: backendSourceFiles.slice(0, 10).map((fullPath) => path.relative(ROOT, fullPath)),
    env: {
      authBaseUrl: process.env.DRAMAFLOW_AUTH_BASE_URL ?? null,
      contentBaseUrl: process.env.DRAMAFLOW_CONTENT_BASE_URL ?? null,
      feedBaseUrl: process.env.DRAMAFLOW_FEED_BASE_URL ?? null,
      progressBaseUrl: process.env.DRAMAFLOW_PROGRESS_BASE_URL ?? null,
      playbackBaseUrl: process.env.DRAMAFLOW_PLAYBACK_BASE_URL ?? null,
      entitlementBaseUrl: process.env.DRAMAFLOW_ENTITLEMENT_BASE_URL ?? null,
      billingBaseUrl: process.env.DRAMAFLOW_BILLING_BASE_URL ?? null,
    },
    blockers,
  };
}

export async function writeEvidence(name, payload) {
  await mkdir(EVIDENCE_DIR, { recursive: true });
  const target = path.join(EVIDENCE_DIR, name);
  await writeFile(target, `${JSON.stringify(payload, null, 2)}\n`, "utf8");
  return target;
}

export async function runFlow(flowId, flowName, evidenceName, requirements = []) {
  const snapshot = await getWorkspaceSnapshot();
  const missingEnv = requirements.filter((key) => !snapshot.env[key]);
  const blockers = [...snapshot.blockers];
  if (missingEnv.length > 0) {
    blockers.push({
      id: `${flowId}-ENV`,
      title: `Missing required environment inputs: ${missingEnv.join(", ")}`,
      area: "environment/config",
      blockerForStaging: true,
      owner: "platform",
    });
  }
  const invalidEnv = [];
  for (const key of requirements) {
    if (!snapshot.env[key]) {
      continue;
    }
    const resolution = await validateResolvableUrl(snapshot.env[key]);
    if (!resolution.ok) {
      invalidEnv.push(key);
      blockers.push({
        id: `${flowId}-DNS-${key}`,
        title: `Unresolvable or invalid environment input: ${key}`,
        area: "environment/config",
        blockerForStaging: true,
        owner: "platform",
      });
    }
  }
  const status = blockers.length > 0 ? "blocked" : "passed";
  const payload = {
    flowId,
    flowName,
    status,
    requirements,
    snapshot,
    blockers,
    invalidEnv,
    nextAction:
      status === "blocked"
        ? "Provide resolvable staging base URLs, inject them into the workspace, and rerun the mobile integration flow."
        : "Flow prerequisites are satisfied. Execute the corresponding Android/manual smoke steps and capture runtime logs.",
  };
  await writeEvidence(evidenceName, payload);
  return payload;
}

export async function writeReadinessAndGoNoGo() {
  const snapshot = await getWorkspaceSnapshot();
  const requiredEnv = [
    "authBaseUrl",
    "contentBaseUrl",
    "feedBaseUrl",
    "progressBaseUrl",
    "playbackBaseUrl",
    "entitlementBaseUrl",
    "billingBaseUrl",
  ];
  const missingEnv = requiredEnv.filter((key) => !snapshot.env[key]);
  const blockers = [...snapshot.blockers];
  if (missingEnv.length > 0) {
    blockers.push({
      id: "MB-READINESS-ENV",
      title: `Missing required environment inputs: ${missingEnv.join(", ")}`,
      area: "environment/config",
      blockerForStaging: true,
      owner: "platform",
    });
  }
  const invalidEnv = [];
  for (const key of requiredEnv) {
    if (!snapshot.env[key]) {
      continue;
    }
    const resolution = await validateResolvableUrl(snapshot.env[key]);
    if (!resolution.ok) {
      invalidEnv.push(key);
      blockers.push({
        id: `MB-READINESS-DNS-${key}`,
        title: `Unresolvable or invalid environment input: ${key}`,
        area: "environment/config",
        blockerForStaging: true,
        owner: "platform",
      });
    }
  }
  const stagingBlockers = blockers.filter((item) => item.blockerForStaging);
  const readiness = {
    generatedAt: snapshot.generatedAt,
    status: stagingBlockers.length === 0 ? "ready" : "blocked",
    stagingBlockerCount: stagingBlockers.length,
    blockers,
    invalidEnv,
    summary:
      stagingBlockers.length === 0
        ? "Ready for Android x backend integration execution."
        : snapshot.blockers.length === 0
          ? "Still blocked by missing staging environment inputs."
          : "Still blocked by environment inputs and missing source trees.",
  };
  const goNoGo = {
    generatedAt: snapshot.generatedAt,
    decision: stagingBlockers.length === 0 ? "go_for_android_backend_staging_integration" : "no_go",
    rationale: readiness.summary,
    stagingBlockers,
  };
  await writeEvidence("mobile-backend-readiness.json", readiness);
  await writeEvidence("mobile-go-no-go.json", goNoGo);
  await writeEvidence("mobile-defect-matrix.json", {
    generatedAt: snapshot.generatedAt,
    count: blockers.length,
    defects: blockers.map((item) => ({
      id: item.id,
      title: item.title,
      area: item.area,
      severity: "P0",
      reproduce_steps: "Inspect current workspace tree and run mobile readiness scripts.",
      expected: "Android and backend source trees should exist with staging config inputs available.",
      actual: item.title,
      suspected_owner: item.owner,
      blocker_for_staging: item.blockerForStaging,
      blocker_for_production: true,
      fix_strategy: "Restore missing source trees and provide staging environment inputs.",
      suggested_fix_phase: "Phase 16 unblock",
    })),
  });
  return { readiness, goNoGo };
}

export async function maybeReadJson(fileName) {
  try {
    const content = await readFile(path.join(EVIDENCE_DIR, fileName), "utf8");
    return JSON.parse(content);
  } catch {
    return null;
  }
}
