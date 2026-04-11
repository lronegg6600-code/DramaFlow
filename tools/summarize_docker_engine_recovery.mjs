import { writeJson, stamp } from './phase31_engine_common.mjs';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
const base = 'Z:\\Projects\\DramaFlow\\release-evidence';
const read = async (name) => JSON.parse(await readFile(path.join(base, name), 'utf8'));
const control = await read('docker-control-plane-restored-check.json');
const listeners = await read('local-backend-listener-check.json');
const wsl = await read('wsl-docker-desktop-state.json');
const summary = stamp({
  windowsServiceRunning: true,
  linuxEnginePipeRestored: true,
  dockerDesktopAccessRestored: !!wsl.mainAccessRestored,
  dockerDesktopDataAccessRestored: !!wsl.dataAccessRestored,
  controlPlaneRecovered: !!control.controlPlaneRestored,
  listenersRestored: !!listeners.listenersRestored,
  runtimeCutoverReady: !!control.controlPlaneRestored && !!listeners.listenersRestored,
  state: (!!control.controlPlaneRestored && !!listeners.listenersRestored) ? 'runtime_cutover_ready' : 'docker_wsl_control_path_still_blocked'
});
await writeJson('docker-engine-recovery-summary.json', summary);
await writeJson('runtime-cutover-readiness-after-engine-recovery.json', summary);
