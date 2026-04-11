import { mkdir, readdir, stat, writeFile, access } from "node:fs/promises";
import { constants } from "node:fs";
import path from "node:path";

const ROOT = path.resolve("Z:/Projects/DramaFlow");
const EVIDENCE_DIR = path.join(ROOT, "release-evidence");

async function exists(target) {
  try {
    await access(target, constants.F_OK);
    return true;
  } catch {
    return false;
  }
}

async function listDirs(dir) {
  try {
    const entries = await readdir(dir, { withFileTypes: true });
    return entries.filter((entry) => entry.isDirectory()).map((entry) => path.join(dir, entry.name));
  } catch {
    return [];
  }
}

async function walkForNames(startDir, targetNames, maxDepth = 4, currentDepth = 0, results = []) {
  if (currentDepth > maxDepth) return results;
  const dirs = await listDirs(startDir);
  for (const dir of dirs) {
    const base = path.basename(dir);
    if (targetNames.includes(base)) {
      results.push(dir);
    }
    await walkForNames(dir, targetNames, maxDepth, currentDepth + 1, results);
  }
  return results;
}

async function fileCount(dir, extensions) {
  let count = 0;
  async function walk(current, depth = 0) {
    if (depth > 6) return;
    let entries = [];
    try {
      entries = await readdir(current, { withFileTypes: true });
    } catch {
      return;
    }
    for (const entry of entries) {
      const full = path.join(current, entry.name);
      if (entry.isDirectory()) {
        if (["build", ".gradle", ".idea", ".kotlin", "node_modules"].includes(entry.name)) continue;
        await walk(full, depth + 1);
      } else if (extensions.some((ext) => full.endsWith(ext))) {
        count += 1;
      }
    }
  }
  if (await exists(dir)) {
    await walk(dir);
  }
  return count;
}

function classifyAndroidSource(snapshot) {
  if (!snapshot.androidDirExists) return "missing";
  if (snapshot.androidSourceCount === 0) return "missing";
  if (snapshot.androidHasGradleSettings && snapshot.androidSourceCount > 20) return "recovered";
  return "partial";
}

function classifyBackendSource(snapshot) {
  if (!snapshot.backendDirExists) return "missing";
  if (snapshot.backendServicesDirExists && snapshot.backendSourceCount > 20) return "recovered";
  if (snapshot.backendSourceCount > 0) return "partial";
  return "missing";
}

async function main() {
  const parent = path.dirname(ROOT);
  const siblings = await listDirs(parent);
  const androidDir = path.join(ROOT, "android");
  const backendDir = path.join(ROOT, "backend");
  const docsDir = path.join(ROOT, "docs");
  const releaseEvidenceDir = path.join(ROOT, "release-evidence");
  const platformIntakeDir = path.join(ROOT, "platform-intake");
  const gitDir = path.join(ROOT, ".git");
  const androidSettings = path.join(androidDir, "settings.gradle.kts");
  const androidBuildGradle = path.join(androidDir, "build.gradle.kts");
  const androidGradleVersions = path.join(androidDir, "gradle", "libs.versions.toml");
  const backendGoWork = path.join(backendDir, "go.work");
  const backendGoMod = path.join(backendDir, "go.mod");
  const backendServicesDir = path.join(backendDir, "services");

  const gitCandidates = [];
  for (const dir of [ROOT, parent, ...siblings]) {
    if (await exists(path.join(dir, ".git"))) {
      gitCandidates.push(dir);
    }
  }

  const candidateScanRoots = [ROOT, parent, ...siblings.filter((dir) => dir !== ROOT)];
  const androidSourceCandidates = [];
  const backendSourceCandidates = [];

  for (const scanRoot of candidateScanRoots) {
    const androidHits = await walkForNames(scanRoot, ["android"], 3);
    for (const hit of androidHits) {
      const sourceCount = await fileCount(hit, [".kt", ".kts", ".properties", ".xml"]);
      androidSourceCandidates.push({
        path: hit,
        sourceCount,
        hasSettingsGradle: await exists(path.join(hit, "settings.gradle.kts")),
        hasBuildGradle: await exists(path.join(hit, "build.gradle.kts")),
        classification: sourceCount === 0 ? "invalid" : "candidate",
      });
    }
    const backendHits = await walkForNames(scanRoot, ["backend"], 3);
    for (const hit of backendHits) {
      const sourceCount = await fileCount(hit, [".go", ".mjs", ".yaml", ".yml"]);
      backendSourceCandidates.push({
        path: hit,
        sourceCount,
        hasGoWork: await exists(path.join(hit, "go.work")),
        hasGoMod: await exists(path.join(hit, "go.mod")),
        hasServicesDir: await exists(path.join(hit, "services")),
        classification: sourceCount === 0 ? "invalid" : "candidate",
      });
    }
  }

  const snapshot = {
    generatedAt: new Date().toISOString(),
    root: ROOT,
    parent,
    siblings,
    gitDirExists: await exists(gitDir),
    gitCandidates,
    androidDirExists: await exists(androidDir),
    androidHasGradleSettings: await exists(androidSettings),
    androidHasBuildGradle: await exists(androidBuildGradle),
    androidHasVersionCatalog: await exists(androidGradleVersions),
    androidSourceCount: await fileCount(androidDir, [".kt", ".kts", ".properties", ".xml"]),
    backendDirExists: await exists(backendDir),
    backendServicesDirExists: await exists(backendServicesDir),
    backendHasGoWork: await exists(backendGoWork),
    backendHasGoMod: await exists(backendGoMod),
    backendSourceCount: await fileCount(backendDir, [".go", ".mjs", ".yaml", ".yml"]),
    docsDirExists: await exists(docsDir),
    releaseEvidenceDirExists: await exists(releaseEvidenceDir),
    platformIntakeDirExists: await exists(platformIntakeDir),
    androidSourceCandidates,
    backendSourceCandidates,
  };

  snapshot.androidStatus = classifyAndroidSource(snapshot);
  snapshot.backendStatus = classifyBackendSource(snapshot);
  snapshot.workspaceStatus =
    snapshot.gitDirExists && snapshot.androidStatus === "recovered" && snapshot.backendStatus === "recovered"
      ? "recovered"
      : "source_missing";

  await mkdir(EVIDENCE_DIR, { recursive: true });
  await writeFile(path.join(EVIDENCE_DIR, "workspace-layout-audit.json"), `${JSON.stringify(snapshot, null, 2)}\n`, "utf8");
  await writeFile(path.join(EVIDENCE_DIR, "git-root-candidates.json"), `${JSON.stringify({ generatedAt: snapshot.generatedAt, candidates: gitCandidates }, null, 2)}\n`, "utf8");
  await writeFile(path.join(EVIDENCE_DIR, "android-source-candidates.json"), `${JSON.stringify({ generatedAt: snapshot.generatedAt, candidates: androidSourceCandidates }, null, 2)}\n`, "utf8");
  await writeFile(path.join(EVIDENCE_DIR, "backend-source-candidates.json"), `${JSON.stringify({ generatedAt: snapshot.generatedAt, candidates: backendSourceCandidates }, null, 2)}\n`, "utf8");

  console.log(JSON.stringify(snapshot, null, 2));
  process.exit(snapshot.workspaceStatus === "recovered" ? 0 : 1);
}

await main();
