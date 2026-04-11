import path from "node:path";
import { EVIDENCE_DIR, ROOT, getNowIso, pathExists, writeJson } from "./source_recovery_common.mjs";

const checks = { settingsGradle: await pathExists(path.join(ROOT, "android", "settings.gradle.kts")), buildGradle: await pathExists(path.join(ROOT, "android", "build.gradle.kts")), appDir: await pathExists(path.join(ROOT, "android", "app")) };
const restored = Object.values(checks).every(Boolean);
const partial = Object.values(checks).some(Boolean) && !restored;
const payload = { generatedAt: getNowIso(), status: restored ? "restored" : partial ? "partial" : "missing", checks };
await writeJson(path.join(EVIDENCE_DIR, "rehydrated-android-tree.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.status === "restored" ? 0 : 1);
