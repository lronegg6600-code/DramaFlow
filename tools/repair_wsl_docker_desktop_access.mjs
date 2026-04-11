import { runPowerShell, writeJson, stamp } from './phase31_engine_common.mjs';
const lxss = await runPowerShell('sc.exe query LxssManager');
const main = await runPowerShell("wsl.exe -d docker-desktop sh -lc 'uname -a && id && test -S /var/run/docker.sock && echo DOCKER_SOCK_OK'");
const data = await runPowerShell("wsl.exe -d docker-desktop-data sh -lc 'uname -a >/dev/null; echo WSL_DATA_OK'");
await writeJson('wsl-access-repair-result.json', stamp({ lxss, dockerDesktopExec: main, dockerDesktopDataExec: data, interpretation: main.status === 'ok' ? 'WSL access to docker-desktop is restored.' : 'WSL access to docker-desktop is still degraded.' }));
