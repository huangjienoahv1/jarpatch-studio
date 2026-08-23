const fs = require('fs');
const path = require('path');

const MESSAGE_VERSION_INVALID = '根 package.json version 不是合法的语义化版本';
const MESSAGE_VERSION_MISMATCH = '版本镜像不一致，请先执行 node scripts/sync-version.js';
const CHECK_ARGUMENT = '--check';
const VERSION_PATTERN = /^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$/;
const rootDir = path.resolve(__dirname, '..');
const rootPackagePath = path.join(rootDir, 'package.json');
const frontendPackagePath = path.join(rootDir, 'frontend', 'package.json');
const rootPomPath = path.join(rootDir, 'pom.xml');
const backendPomPath = path.join(rootDir, 'backend', 'pom.xml');

/**
 * 读取 JSON 文件。
 *
 * @param {string} filePath 文件路径
 * @returns {object} JSON 对象
 */
function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'));
}

/**
 * 使用稳定的两空格格式写入 JSON 文件。
 *
 * @param {string} filePath 文件路径
 * @param {object} value JSON 对象
 */
function writeJson(filePath, value) {
  fs.writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8');
}

/**
 * 从根 Maven POM 读取 revision 镜像。
 *
 * @param {string} content POM 文本
 * @returns {string} revision
 */
function readRevision(content) {
  const match = content.match(/<revision>([^<]+)<\/revision>/);
  return match ? match[1] : '';
}

/**
 * 校验后端父版本使用 Maven CI-friendly revision 占位符。
 *
 * @param {string} content 后端 POM 文本
 * @returns {boolean} 是否符合单一来源合同
 */
function backendUsesRevision(content) {
  return /<parent>[\s\S]*?<version>\$\{revision}<\/version>[\s\S]*?<\/parent>/.test(content);
}

const rootPackage = readJson(rootPackagePath);
const frontendPackage = readJson(frontendPackagePath);
const rootPom = fs.readFileSync(rootPomPath, 'utf8');
const backendPom = fs.readFileSync(backendPomPath, 'utf8');
const version = rootPackage.version;

if (!VERSION_PATTERN.test(version)) {
  throw new Error(`${MESSAGE_VERSION_INVALID}: ${version}`);
}

const consistent = frontendPackage.version === version
  && readRevision(rootPom) === version
  && backendUsesRevision(backendPom);

if (process.argv.includes(CHECK_ARGUMENT)) {
  if (!consistent) {
    throw new Error(`${MESSAGE_VERSION_MISMATCH}: ${version}`);
  }
  process.stdout.write(`版本一致性检查通过: ${version}\n`);
  process.exit(0);
}

frontendPackage.version = version;
writeJson(frontendPackagePath, frontendPackage);
fs.writeFileSync(rootPom, rootPom.replace(/<revision>[^<]+<\/revision>/, `<revision>${version}</revision>`), 'utf8');
if (!backendUsesRevision(backendPom)) {
  throw new Error('backend/pom.xml 必须使用 ${revision} 作为父版本');
}
process.stdout.write(`已从根 package.json 同步版本: ${version}\n`);
