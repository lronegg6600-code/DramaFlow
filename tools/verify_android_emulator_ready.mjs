import path from "node:path";
import {
  ADB_PATH,
  APK_PATH,
  EMULATOR_PATH,
  EVIDENCE_DIR,
  SDK_ROOT,
  fileExists,
  getNowIso,
  getReadyDevice,
  listAvds,
  readAvdConfig,
  writeJson,
} from "./android_local_common.mjs";

const avdListing = await listAvds();
const avdConfigs = await Promise.all(avdListing.avds.map((avdName) => readAvdConfig(avdName)));
const readyDevice = await getReadyDevice();
const apkExists = await fileExists(APK_PATH);

const payload = {
  generatedAt: getNowIso(),
  adbPath: ADB_PATH,
  emulatorPath: EMULATOR_PATH,
  sdkRoot: SDK_ROOT,
  adbAvailable: await fileExists(ADB_PATH),
  emulatorAvailable: await fileExists(EMULATOR_PATH),
  apkExists,
  apkPath: path.relative(process.cwd(), APK_PATH),
  avdListing,
  avdConfigs,
  adb: readyDevice.adb,
  readyDevice: readyDevice.ready
    ? {
        serial: readyDevice.device.serial,
        details: readyDevice.device.details,
      }
    : null,
  ready: readyDevice.ready,
  blockerSummary: readyDevice.ready
    ? "Android emulator/device is ready for local App smoke."
    : "No booted Android emulator/device is available. Existing AVDs reference missing Android 35 system images, and the current sdkmanager runtime cannot auto-repair them.",
};

await writeJson(path.join(EVIDENCE_DIR, "android-emulator-readiness.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.ready ? 0 : 1);
