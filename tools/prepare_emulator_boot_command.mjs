import { prepareEmulatorBootCommand, RUNTIME_EVIDENCE, writeJson } from "./android_runtime_target_common.mjs";

await writeJson(RUNTIME_EVIDENCE.summary.replace("android-runtime-recovery-summary", "android-emulator-boot-command"), await prepareEmulatorBootCommand());
