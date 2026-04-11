import { runPowerShell, writeJson, stamp } from './phase31_engine_common.mjs';
const list = await runPowerShell('wsl.exe --list --verbose');
const main = await runPowerShell("wsl.exe -d docker-desktop sh -lc 'uname -a && id && test -S /var/run/docker.sock && echo DOCKER_SOCK_OK'");
const data = await runPowerShell("wsl.exe -d docker-desktop-data sh -lc 'uname -a >/dev/null; echo WSL_DATA_OK'");
await writeJson('wsl-docker-desktop-state.json', stamp({ list, dockerDesktopExec: main, dockerDesktopDataExec: data, mainAccessRestored: main.status === 'ok' && /DOCKER_SOCK_OK/.test(main.stdout || ''), dataAccessRestored: data.status === 'ok' && /WSL_DATA_OK/.test(data.stdout || '') }));
