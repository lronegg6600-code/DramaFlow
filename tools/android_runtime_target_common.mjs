import fs from "node:fs/promises";
import path from "node:path";
import {
  ROOT,
  EVIDENCE_DIR,
  DOCS_DIR,
  APK_PATH,
  APP_ACTIVITY,
  APP_PACKAGE,
  ADB_PATH,
  AVD_HOME,
  EMULATOR_PATH,
  SDK_ROOT,
  SCREENSHOT_DIR,
  RECORDING_DIR,
  LOGCAT_DIR,
  NETWORK_DIR,
  REPORTS_DIR,
  ensureAndroidEvidenceDirs,
  fileExists,
  getAdbDevices,
  getBootCompleted,
  getReadyDevice,
  getNowIso,
  listAvds,
  readAvdConfig,
  runAdb,
  runCommand,
  writeJson,
  writeMarkdownReport,
} from "./android_local_common.mjs";
import { ensureDir } from "./staging_input_common.mjs";

export {
  ROOT,
  EVIDENCE_DIR,
  DOCS_DIR,
  APK_PATH,
  APP_ACTIVITY,
  APP_PACKAGE,
  ADB_PATH,
  AVD_HOME,
  EMULATOR_PATH,
  SDK_ROOT,
  SCREENSHOT_DIR,
  RECORDING_DIR,
  LOGCAT_DIR,
  NETWORK_DIR,
  REPORTS_DIR,
  ensureAndroidEvidenceDirs,
  fileExists,
  getAdbDevices,
  getBootCompleted,
  getReadyDevice,
  getNowIso,
  listAvds,
  readAvdConfig,
  runAdb,
  runCommand,
  writeJson,
  writeMarkdownReport,
  ensureDir,
};

export const RUNTIME_EVIDENCE = {
  diagnosis: path.join(EVIDENCE_DIR, "android-runtime-target-diagnosis.json"),
  sdkAssets: path.join(EVIDENCE_DIR, "android-sdk-asset-inspection.json"),
  avdInspection: path.join(EVIDENCE_DIR, "android-avd-inspection.json"),
  avdRepair: path.join(EVIDENCE_DIR, "android-avd-repair-result.json"),
  imageBinding: path.join(EVIDENCE_DIR, "android-system-image-binding-result.json"),
  sdkmanagerCompatibility: path.join(EVIDENCE_DIR, "android-sdkmanager-compatibility.json"),
  bootedTarget: path.join(EVIDENCE_DIR, "android-booted-target-check.json"),
  apkInstall: path.join(EVIDENCE_DIR, "android-apk-install-result.json"),
  appLaunch: path.join(EVIDENCE_DIR, "android-app-launch-result.json"),
  feedDetail: path.join(EVIDENCE_DIR, "android-feed-detail-app-smoke.json"),
  playback: path.join(EVIDENCE_DIR, "android-playback-app-smoke.json"),
  billing: path.join(EVIDENCE_DIR, "android-billing-entitlement-app-smoke.json"),
  revokeRestore: path.join(EVIDENCE_DIR, "android-revoke-restore-app-smoke.json"),
  appEvidence: path.join(EVIDENCE_DIR, "android-app-evidence-index.json"),
  summary: path.join(EVIDENCE_DIR, "android-runtime-recovery-summary.json"),
};

export const RUNTIME_DOCS = {
  targetReport: path.join(DOCS_DIR, "android-runtime-target-report.md"),
  avdRepair: path.join(DOCS_DIR, "android-avd-repair-report.md"),
  appSmoke: path.join(DOCS_DIR, "android-app-smoke-execution-report.md"),
  defectMatrix: path.join(DOCS_DIR, "android-app-defect-matrix.md"),
  goNoGo: path.join(DOCS_DIR, "android-app-go-no-go.md"),
  phase27: path.join(DOCS_DIR, "phase27-runtime-bringup-report.md"),
};

export async function inspectSdkAssets() {
  const sdkmanagerPath = path.join(SDK_ROOT, "cmdline-tools", "latest", "bin", "sdkmanager.bat");
  const avdmanagerPath = path.join(SDK_ROOT, "cmdline-tools", "latest", "bin", "avdmanager.bat");
  const systemImage33 = path.join(SDK_ROOT, "system-images", "android-33", "google_apis", "x86_64");
  const systemImage35Play = path.join(SDK_ROOT, "system-images", "android-35", "google_apis_playstore", "x86_64");
  const payload = {
    generatedAt: getNowIso(),
    sdkRoot: SDK_ROOT,
    adbExists: await fileExists(ADB_PATH),
    emulatorExists: await fileExists(EMULATOR_PATH),
    sdkmanagerPath,
    sdkmanagerExists: await fileExists(sdkmanagerPath),
    avdmanagerPath,
    avdmanagerExists: await fileExists(avdmanagerPath),
    systemImages: [
      { path: systemImage33, exists: await fileExists(systemImage33) },
      { path: systemImage35Play, exists: await fileExists(systemImage35Play) },
    ],
  };
  await writeJson(RUNTIME_EVIDENCE.sdkAssets, payload);
  return payload;
}

export async function inspectAvdDefinitions() {
  const listed = await listAvds();
  const configs = [];
  for (const avd of listed.avds) {
    configs.push(await readAvdConfig(avd));
  }
  const payload = {
    generatedAt: getNowIso(),
    avdHome: AVD_HOME,
    listResultAvailable: listed.available,
    avds: configs,
    rawOutput: listed.output,
  };
  await writeJson(RUNTIME_EVIDENCE.avdInspection, payload);
  return payload;
}

export async function verifySdkmanagerCompatibility() {
  const runBatch = (batchPath, args) =>
    runCommand("C:\\Windows\\System32\\cmd.exe", ["/c", batchPath, ...args], { timeoutMs: 45000, cwd: ROOT });
  const sdkmanagerPath = path.join(SDK_ROOT, "cmdline-tools", "latest", "bin", "sdkmanager.bat");
  const avdmanagerPath = path.join(SDK_ROOT, "cmdline-tools", "latest", "bin", "avdmanager.bat");
  const legacySdkmanagerPath = path.join(SDK_ROOT, "tools", "bin", "sdkmanager.bat");
  const legacyAvdmanagerPath = path.join(SDK_ROOT, "tools", "bin", "avdmanager.bat");
  const sdkmanager = (await fileExists(sdkmanagerPath))
    ? await runBatch(sdkmanagerPath, ["--list"])
    : { code: 127, stdout: "", stderr: "sdkmanager.bat not found", timedOut: false };
  const avdmanager = (await fileExists(avdmanagerPath))
    ? await runBatch(avdmanagerPath, ["list", "avd"])
    : { code: 127, stdout: "", stderr: "avdmanager.bat not found", timedOut: false };
  const legacySdkmanager = (await fileExists(legacySdkmanagerPath))
    ? await runBatch(legacySdkmanagerPath, ["--list"])
    : { code: 127, stdout: "", stderr: "legacy sdkmanager.bat not found", timedOut: false };
  const legacyAvdmanager = (await fileExists(legacyAvdmanagerPath))
    ? await runBatch(legacyAvdmanagerPath, ["list", "avd"])
    : { code: 127, stdout: "", stderr: "legacy avdmanager.bat not found", timedOut: false };
  const payload = {
    generatedAt: getNowIso(),
    sdkmanager,
    avdmanager,
    legacySdkmanager,
    legacyAvdmanager,
    compatible: sdkmanager.code === 0 && avdmanager.code === 0,
  };
  await writeJson(RUNTIME_EVIDENCE.sdkmanagerCompatibility, payload);
  return payload;
}

export async function diagnoseRuntimeTarget() {
  const adb = await getAdbDevices();
  const ready = await getReadyDevice();
  const sdkAssets = await inspectSdkAssets();
  const avds = await inspectAvdDefinitions();
  const payload = {
    generatedAt: getNowIso(),
    adbPath: ADB_PATH,
    emulatorPath: EMULATOR_PATH,
    apkPath: APK_PATH,
    adb: {
      code: adb.result.code,
      stdout: adb.result.stdout,
      stderr: adb.result.stderr,
      devices: adb.devices,
    },
    bootedTargetAvailable: ready.ready,
    bootedTarget: ready.ready
      ? {
          serial: ready.device.serial,
          details: ready.device.details,
          bootCompleted: ready.boot.bootCompleted,
        }
      : null,
    blockers: ready.ready
      ? []
      : [
          "android_runtime_target_not_booted",
          ...(!sdkAssets.systemImages.some((entry) => entry.exists) ? ["system_image_assets_missing"] : []),
        ],
    sdkAssets,
    avdSummary: {
      count: avds.avds.length,
      brokenImageBindings: avds.avds.filter((entry) => entry && !entry.imageSysdirExists).map((entry) => entry.avdName),
      brokenSkinBindings: avds.avds.filter((entry) => entry && !entry.skinPathExists).map((entry) => entry.avdName),
    },
  };
  await writeJson(RUNTIME_EVIDENCE.diagnosis, payload);
  return payload;
}

export async function repairAvdPaths() {
  const inspection = await inspectAvdDefinitions();
  const broken = inspection.avds.filter((entry) => entry && !entry.skinPathExists);
  const payload = {
    generatedAt: getNowIso(),
    status: broken.length ? "not_applied" : "not_required",
    reason: broken.length
      ? "Physical device path was selected; AVD skin repair left as follow-up because no emulator path was required for this run."
      : "No broken skin bindings detected.",
    brokenSkinBindings: broken.map((entry) => ({ avdName: entry.avdName, skinPath: entry.skinPath })),
  };
  await writeJson(RUNTIME_EVIDENCE.avdRepair, payload);
  return payload;
}

export async function repairAvdSystemImageBinding() {
  const inspection = await inspectAvdDefinitions();
  const broken = inspection.avds.filter((entry) => entry && !entry.imageSysdirExists);
  const payload = {
    generatedAt: getNowIso(),
    status: broken.length ? "not_applied" : "not_required",
    reason: broken.length
      ? "Physical device path was selected; AVD system image rebinding left blocked on missing SDK assets."
      : "No broken system image bindings detected.",
    brokenSystemImageBindings: broken.map((entry) => ({
      avdName: entry.avdName,
      imageSysdir: entry.imageSysdir,
      target: entry.target,
    })),
  };
  await writeJson(RUNTIME_EVIDENCE.imageBinding, payload);
  return payload;
}

export async function prepareEmulatorBootCommand() {
  const inspection = await inspectAvdDefinitions();
  const firstRunnable = inspection.avds.find((entry) => entry && entry.imageSysdirExists && entry.skinPathExists);
  return {
    generatedAt: getNowIso(),
    available: Boolean(firstRunnable),
    command: firstRunnable ? `"${EMULATOR_PATH}" -avd ${firstRunnable.avdName}` : null,
    reason: firstRunnable ? null : "No runnable AVD binding is currently available.",
  };
}

export async function verifyBootedAndroidTarget() {
  const ready = await getReadyDevice();
  const payload = {
    generatedAt: getNowIso(),
    booted: ready.ready,
    targetType: ready.ready && !ready.device.details.includes("emulator") ? "physical_device" : ready.ready ? "emulator" : "none",
    serial: ready.ready ? ready.device.serial : null,
    details: ready.ready ? ready.device.details : null,
    bootCompleted: ready.ready ? ready.boot.bootCompleted : false,
    blockers: ready.ready ? [] : ["android_runtime_target_not_booted"],
  };
  await writeJson(RUNTIME_EVIDENCE.bootedTarget, payload);
  return payload;
}

export async function installDebugApkToTarget(serial) {
  const result = await runAdb(["-s", serial, "install", "-r", APK_PATH], { timeoutMs: 180000 });
  const payload = {
    generatedAt: getNowIso(),
    serial,
    apkPath: APK_PATH,
    status: result.code === 0 ? "installed" : "failed",
    stdout: result.stdout,
    stderr: result.stderr,
  };
  await writeJson(RUNTIME_EVIDENCE.apkInstall, payload);
  return payload;
}

export async function launchAppOnTarget(serial) {
  const result = await runAdb(["-s", serial, "shell", "am", "start", "-W", "-n", `${APP_PACKAGE}/${APP_ACTIVITY}`], { timeoutMs: 60000 });
  const payload = {
    generatedAt: getNowIso(),
    serial,
    status: result.code === 0 ? "launched" : "failed",
    stdout: result.stdout,
    stderr: result.stderr,
  };
  await writeJson(RUNTIME_EVIDENCE.appLaunch, payload);
  return payload;
}

export async function collectAppRuntimeEvidence() {
  await ensureAndroidEvidenceDirs();
  const screenshots = (await fs.readdir(SCREENSHOT_DIR)).filter((name) => name.toLowerCase().endsWith(".png"));
  const recordings = (await fs.readdir(RECORDING_DIR)).filter((name) => name.toLowerCase().endsWith(".mp4"));
  const logcats = (await fs.readdir(LOGCAT_DIR)).filter((name) => name.toLowerCase().endsWith(".log"));
  const network = (await fs.readdir(NETWORK_DIR)).filter((name) => name.toLowerCase().endsWith(".log") || name.toLowerCase().endsWith(".txt"));
  const payload = {
    generatedAt: getNowIso(),
    screenshotCount: screenshots.length,
    recordingCount: recordings.length,
    logcatCount: logcats.length,
    networkArtifactCount: network.length,
    screenshots,
    recordings,
    logcats,
    network,
  };
  await writeJson(RUNTIME_EVIDENCE.appEvidence, payload);
  return payload;
}

export async function writePhase27Docs({
  summary,
  diagnosis,
  sdkCompatibility,
  feedDetail,
  playback,
  billing,
  revokeRestore,
  defects,
}) {
  await writeMarkdownReport(RUNTIME_DOCS.targetReport, [
    "# Android Runtime Target Report",
    "",
    `- Generated: ${summary.generatedAt}`,
    `- Booted target available: \`${summary.bootedTargetAvailable ? "yes" : "no"}\``,
    `- Target type: \`${summary.targetType}\``,
    `- Target serial: \`${summary.targetSerial ?? "none"}\``,
    `- APK installed: \`${summary.apkInstalled ? "yes" : "no"}\``,
    `- App launched: \`${summary.appLaunched ? "yes" : "no"}\``,
    "",
    "## Runtime Diagnosis",
    `- adb devices found: \`${diagnosis.adb.devices.length}\``,
    `- Booted target: \`${summary.bootedTargetAvailable ? "available" : "missing"}\``,
    `- SDK manager compatible: \`${sdkCompatibility.compatible ? "yes" : "no"}\``,
  ]);

  await writeMarkdownReport(RUNTIME_DOCS.avdRepair, [
    "# Android AVD Repair Report",
    "",
    `- Generated: ${summary.generatedAt}`,
    `- AVD path repair: \`${summary.avdRepairStatus}\``,
    `- System image binding repair: \`${summary.imageBindingStatus}\``,
    `- Emulator path chosen for this run: \`${summary.targetType === "emulator" ? "yes" : "no"}\``,
  ]);

  await writeMarkdownReport(RUNTIME_DOCS.appSmoke, [
    "# Android App Smoke Execution Report",
    "",
    `- Feed / Detail: \`${feedDetail.status}\``,
    `- Playback: \`${playback.status}\``,
    `- Billing / Entitlement: \`${billing.status}\``,
    `- Revoke / Restore: \`${revokeRestore.status}\``,
    "",
    "## Notes",
    `- Billing flow note: ${billing.summary}`,
    `- Revoke/restore note: ${revokeRestore.summary}`,
  ]);

  const defectLines = [
    "# Android App Defect Matrix",
    "",
    "| id | title | area | severity | suspected_owner | blocker_for_staging | blocker_for_production | fix_strategy |",
    "| --- | --- | --- | --- | --- | --- | --- | --- |",
  ];
  for (const defect of defects) {
    defectLines.push(`| ${defect.id} | ${defect.title} | ${defect.area} | ${defect.severity} | ${defect.suspected_owner} | ${defect.blocker_for_staging ? "yes" : "no"} | ${defect.blocker_for_production ? "yes" : "no"} | ${defect.fix_strategy} |`);
  }
  await writeMarkdownReport(RUNTIME_DOCS.defectMatrix, defectLines);

  await writeMarkdownReport(RUNTIME_DOCS.goNoGo, [
    "# Android App Go / No-Go",
    "",
    `- Decision: \`${summary.localAppSmokePass ? "Go for local app smoke baseline" : "No-go for local app smoke sign-off"}\``,
    `- Booted target available: \`${summary.bootedTargetAvailable ? "yes" : "no"}\``,
    `- App launched: \`${summary.appLaunched ? "yes" : "no"}\``,
    `- Feed / Detail: \`${feedDetail.status}\``,
    `- Playback: \`${playback.status}\``,
    `- Billing / Entitlement: \`${billing.status}\``,
    `- Revoke / Restore: \`${revokeRestore.status}\``,
    `- Staging ready: \`no\``,
  ]);

  await writeMarkdownReport(RUNTIME_DOCS.phase27, [
    "# Phase 27 Runtime Bring-Up Report",
    "",
    `- Generated: ${summary.generatedAt}`,
    `- Current state: \`${summary.currentState}\``,
    `- Booted target: \`${summary.bootedTargetAvailable ? "yes" : "no"}\``,
    `- Local script pass only: \`${summary.localScriptPassOnly ? "yes" : "no"}\``,
    `- Local app smoke pass: \`${summary.localAppSmokePass ? "yes" : "no"}\``,
    `- New app defect count: \`${defects.length}\``,
  ]);
}
