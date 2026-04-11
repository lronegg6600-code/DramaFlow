import { launchAppOnTarget, verifyBootedAndroidTarget } from "./android_runtime_target_common.mjs";

const target = await verifyBootedAndroidTarget();
if (!target.booted || !target.serial) {
  throw new Error("No booted Android target available.");
}
await launchAppOnTarget(target.serial);
