const path = require('path');
const { spawnSync } = require('child_process');

/**
 * 复用固定 electron-builder 的 NSIS 编译器生成独立诊断器，不给目标机器增加运行时依赖。
 */
async function main() {
  const [releaseDirectory, applicationName, target] = process.argv.slice(2);
  if (!releaseDirectory || !applicationName || path.basename(applicationName) !== applicationName
      || !['windows', 'windows7'].includes(target)) {
    throw new Error('用法: node build-startup-diagnostics.js <releaseDirectory> <applicationExeName> <windows|windows7>');
  }
  const frontendRoot = path.resolve(__dirname, '..', 'frontend');
  const { getMakeNsisPath } = require(path.join(frontendRoot, 'node_modules', 'app-builder-lib', 'out', 'toolsets', 'windows'));
  const compiler = await getMakeNsisPath();
  const output = path.resolve(releaseDirectory, 'JarPatch-Startup-Diagnostics.exe');
  const args = ['-WX', '-V2', '-INPUTCHARSET', 'UTF8', `-DOUTPUT_FILE=${output}`, `-DAPP_EXE=${applicationName}`];
  if (target === 'windows7') args.push('-DWINDOWS7');
  args.push(path.join(__dirname, 'startup-diagnostics.nsi'));
  const result = spawnSync(compiler.path, args, { env: { ...process.env, ...compiler.env }, stdio: 'inherit', windowsHide: true });
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error(`启动诊断器构建失败，退出码：${result.status}`);
  process.stdout.write(`启动诊断器已生成：${output}\n`);
}

main().catch((error) => { process.stderr.write(`${error.stack || error.message}\n`); process.exitCode = 1; });
