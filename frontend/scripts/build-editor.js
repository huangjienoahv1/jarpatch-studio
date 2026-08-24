const esbuild = require('esbuild');
const path = require('path');

const frontendRoot = path.resolve(__dirname, '..');
const sourceRoot = path.join(frontendRoot, 'src');
const outputRoot = path.join(sourceRoot, 'generated');

/**
 * 构建 Monaco 编辑器主线程与 Worker 脚本。
 *
 * 入口由 npm start 和各平台发布命令调用，产物固定写入 src/generated，Electron
 * 打包时直接收集该稳定目录，不依赖运行机器上的全局前端工具。
 *
 * @returns {Promise<void>} 构建完成后结束
 */
async function buildEditor() {
  await esbuild.build({
    entryPoints: [path.join(sourceRoot, 'editor-bridge.js')],
    bundle: true,
    outfile: path.join(outputRoot, 'editor-bridge.js'),
    format: 'iife',
    platform: 'browser',
    target: ['chrome138'],
    sourcemap: false,
    legalComments: 'none'
  });
  await esbuild.build({
    entryPoints: [path.join(sourceRoot, 'editor-worker.js')],
    bundle: true,
    outfile: path.join(outputRoot, 'editor-worker.js'),
    format: 'iife',
    platform: 'browser',
    target: ['chrome138'],
    sourcemap: false,
    legalComments: 'none'
  });
}

buildEditor().catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`);
  process.exitCode = 1;
});
