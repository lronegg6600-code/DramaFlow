import fs from "node:fs/promises";
import path from "node:path";
import {
  ROOT,
  EVIDENCE_DIR,
  DOCS_DIR,
  LOCAL_DEBUG_URLS,
  LOCAL_DEBUG_ENV_FILE,
  getNowIso,
  writeJson,
  writeText,
  readJsonIfExists,
  runCommand,
} from "./local_docker_common.mjs";
import { ensureDir } from "./staging_input_common.mjs";

export {
  ROOT,
  EVIDENCE_DIR,
  DOCS_DIR,
  LOCAL_DEBUG_URLS,
  LOCAL_DEBUG_ENV_FILE,
  getNowIso,
  writeJson,
  writeText,
  ensureDir,
  readJsonIfExists,
  runCommand,
};

export const ANDROID_DIR = path.join(ROOT, "android");
export const APP_DIR = path.join(ANDROID_DIR, "app");
export const APK_PATH = path.join(APP_DIR, "build", "outputs", "apk", "debug", "app-debug.apk");
export const APP_PACKAGE = "com.dramaflow.app.debug";
export const APP_ACTIVITY = "com.dramaflow.app.MainActivity";
export const SDK_ROOT = process.env.ANDROID_SDK_ROOT || "C:\\Users\\admin\\AppData\\Local\\Android\\Sdk";
export const ADB_PATH = path.join(SDK_ROOT, "platform-tools", "adb.exe");
export const EMULATOR_PATH = path.join(SDK_ROOT, "emulator", "emulator.exe");
export const AVD_HOME = path.join("C:\\Users\\admin", ".android", "avd");
export const INTEGRATION_DIR = path.join(ANDROID_DIR, "integration");
export const EVIDENCE_BASE_DIR = path.join(INTEGRATION_DIR, "evidence");
export const SCREENSHOT_DIR = path.join(EVIDENCE_BASE_DIR, "screenshots");
export const RECORDING_DIR = path.join(EVIDENCE_BASE_DIR, "recordings");
export const LOGCAT_DIR = path.join(EVIDENCE_BASE_DIR, "logcat");
export const NETWORK_DIR = path.join(EVIDENCE_BASE_DIR, "network");
export const REPORTS_DIR = path.join(INTEGRATION_DIR, "reports");

export async function ensureAndroidEvidenceDirs() {
  await ensureDir(SCREENSHOT_DIR);
  await ensureDir(RECORDING_DIR);
  await ensureDir(LOGCAT_DIR);
  await ensureDir(NETWORK_DIR);
  await ensureDir(REPORTS_DIR);
}

export async function fileExists(targetPath) {
  try {
    await fs.access(targetPath);
    return true;
  } catch {
    return false;
  }
}

export async function readText(targetPath) {
  return fs.readFile(targetPath, "utf8");
}

export async function runAdb(args, options = {}) {
  return runCommand(ADB_PATH, args, options);
}

export async function runEmulator(args, options = {}) {
  return runCommand(EMULATOR_PATH, args, options);
}

export function buildBlockedSmokePayload({ flowId, flowName, reason, blockers }) {
  return {
    generatedAt: getNowIso(),
    flowId,
    flowName,
    status: "blocked",
    blockers,
    reason,
  };
}

export async function listAvds() {
  if (!(await fileExists(EMULATOR_PATH))) {
    return { available: false, avds: [], output: "emulator.exe not found" };
  }
  const result = await runEmulator(["-list-avds"], { timeoutMs: 15000 });
  const avds = result.stdout
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
  return { available: result.code === 0, avds, output: result.stdout || result.stderr };
}

export async function readAvdConfig(avdName) {
  const configPath = path.join(AVD_HOME, `${avdName}.avd`, "config.ini");
  if (!(await fileExists(configPath))) {
    return null;
  }
  const content = await readText(configPath);
  const pairs = Object.fromEntries(
    content
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith("#") && line.includes("="))
      .map((line) => {
        const [key, ...rest] = line.split("=");
        return [key, rest.join("=")];
      }),
  );
  const imageSysdir = pairs["image.sysdir.1"] ? path.join(SDK_ROOT, pairs["image.sysdir.1"]) : null;
  const skinPath = pairs["skin.path"] && pairs["skin.path"] !== "_no_skin" ? pairs["skin.path"] : null;
  return {
    avdName,
    configPath,
    imageSysdir,
    imageSysdirExists: imageSysdir ? await fileExists(imageSysdir) : false,
    skinPath,
    skinPathExists: skinPath ? await fileExists(skinPath) : true,
    target: pairs.target || null,
    abiType: pairs["abi.type"] || null,
    playStoreEnabled: pairs["PlayStore.enabled"] || null,
  };
}

export async function getAdbDevices() {
  const result = await runAdb(["devices", "-l"], { timeoutMs: 15000 });
  const lines = result.stdout.split(/\r?\n/).slice(1).map((line) => line.trim()).filter(Boolean);
  const devices = lines.map((line) => {
    const [serial, state, ...rest] = line.split(/\s+/);
    return { serial, state, details: rest.join(" ") };
  });
  return { result, devices };
}

export async function getBootCompleted(serial) {
  const result = await runAdb(["-s", serial, "shell", "getprop", "sys.boot_completed"], { timeoutMs: 15000 });
  return {
    serial,
    bootCompleted: result.code === 0 && result.stdout.trim() === "1",
    result,
  };
}

export async function getReadyDevice() {
  const adb = await getAdbDevices();
  for (const device of adb.devices) {
    if (device.state !== "device") {
      continue;
    }
    const boot = await getBootCompleted(device.serial);
    if (boot.bootCompleted) {
      return {
        adb,
        device,
        boot,
        ready: true,
      };
    }
  }
  return { adb, ready: false };
}

export async function writeMarkdownReport(targetPath, lines) {
  await writeText(targetPath, `${lines.join("\n")}\n`);
}
