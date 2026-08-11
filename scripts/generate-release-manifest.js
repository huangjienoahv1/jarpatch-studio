const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const EXPECTED_ARGUMENT_COUNT = 7;
const MANIFEST_FILE_NAME = 'release-manifest.json';

/**
 * 计算发布文件的 SHA-256。
 *
 * @param {string} filePath 发布文件绝对路径
 * @returns {Promise<string>} 小写十六进制哈希
 */
function sha256(filePath) {
  return new Promise((resolve, reject) => {
    const hash = crypto.createHash('sha256');
    const input = fs.createReadStream(filePath);
    input.on('error', reject);
    input.on('data', (chunk) => hash.update(chunk));
    input.on('end', () => resolve(hash.digest('hex')));
  });
}

/**
 * 生成跨平台统一格式的发布清单。
 *
 * @returns {Promise<void>} 清单写入发布目录后完成
 */
async function main() {
  if (process.argv.length !== EXPECTED_ARGUMENT_COUNT) {
    throw new Error('用法: node generate-release-manifest.js <platform> <architecture> <releaseDir> <javaVersion> <npmVersion>');
  }
  const [, , platform, architecture, releaseDirectoryArgument, javaVersion, npmVersion] = process.argv;
  const releaseDirectory = path.resolve(releaseDirectoryArgument);
  const entries = fs.readdirSync(releaseDirectory, { withFileTypes: true })
    .filter((entry) => entry.isFile() && entry.name !== MANIFEST_FILE_NAME)
    .sort((left, right) => left.name.localeCompare(right.name));
  if (entries.length === 0) {
    throw new Error(`发布目录没有可登记的产物：${releaseDirectory}`);
  }
  const artifacts = [];
  for (const entry of entries) {
    const filePath = path.join(releaseDirectory, entry.name);
    const statistics = fs.statSync(filePath);
    artifacts.push({
      name: entry.name,
      size: statistics.size,
      sha256: await sha256(filePath)
    });
  }
  const manifest = {
    product: 'JarPatch Studio',
    version: require(path.resolve(__dirname, '..', 'package.json')).version,
    platform,
    architecture,
    builtAt: new Date().toISOString(),
    javaVersion,
    nodeVersion: process.version,
    npmVersion,
    artifacts
  };
  fs.writeFileSync(path.join(releaseDirectory, MANIFEST_FILE_NAME), `${JSON.stringify(manifest, null, 2)}\n`, 'utf8');
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
