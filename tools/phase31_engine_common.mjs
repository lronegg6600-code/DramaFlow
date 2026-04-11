import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';

const execFileAsync = promisify(execFile);
export const repoRoot = 'Z:\\Projects\\DramaFlow';
export const evidenceDir = path.join(repoRoot, 'release-evidence');

export async function ensureDir(dir) {
  await mkdir(dir, { recursive: true });
}

export async function writeJson(name, data) {
  await ensureDir(evidenceDir);
  const file = path.join(evidenceDir, name);
  await writeFile(file, JSON.stringify(data, null, 2));
  return file;
}

export async function runPowerShell(command, timeoutMs = 120000) {
  try {
    const { stdout, stderr } = await execFileAsync('powershell.exe', ['-NoProfile', '-Command', command], {
      cwd: repoRoot,
      timeout: timeoutMs,
      maxBuffer: 1024 * 1024 * 20,
      windowsHide: true,
    });
    return { status: 'ok', exitCode: 0, stdout, stderr };
  } catch (error) {
    return {
      status: 'failed',
      exitCode: typeof error.code === 'number' ? error.code : 1,
      stdout: error.stdout ?? '',
      stderr: error.stderr ?? (error.message || ''),
    };
  }
}

export function stamp(extra = {}) {
  return { generatedAt: new Date().toISOString(), ...extra };
}
