const { app, safeStorage } = require('electron');
const fs = require('fs');
const path = require('path');
const { sshPasswordTarget, assertSshPassword } = require('./runtime-password-ssh');

const FILE_NAME = 'runtime-ssh-hosts.json';

function hostId({ host, username, port }) {
  return `${username}@${host}:${port}`;
}

function readHosts() {
  try {
    const entries = JSON.parse(fs.readFileSync(path.join(app.getPath('userData'), FILE_NAME), 'utf8'));
    if (!Array.isArray(entries)) throw new Error('Invalid SSH host list');
    return new Map(entries.map(({ host, username, port, encryptedPassword }) => {
      const target = sshPasswordTarget({ host, username, port });
      if (typeof encryptedPassword !== 'string' || !encryptedPassword) throw new Error('Invalid saved credential');
      return [hostId(target), { ...target, encryptedPassword }];
    }));
  } catch (error) {
    if (error.code === 'ENOENT') return new Map();
    throw new Error('无法读取已保存的 SSH 主机，请检查本机应用数据目录');
  }
}

function writeHosts(hosts) {
  const directory = app.getPath('userData');
  const file = path.join(directory, FILE_NAME);
  const temporary = file + '.tmp';
  try {
    fs.mkdirSync(directory, { recursive: true });
    fs.writeFileSync(temporary, JSON.stringify([...hosts.values()], null, 2) + '\n', { encoding: 'utf8', mode: 0o600 });
    fs.renameSync(temporary, file);
  } catch {
    try { fs.rmSync(temporary, { force: true }); } catch { /* 保留原始保存错误 */ }
    throw new Error('无法写入 SSH 主机配置，请检查本机应用数据目录');
  }
}

function publicHosts(hosts) {
  return [...hosts].map(([id, { host, username, port }]) => ({ id, host, username, port }));
}

function requireCredentialStorage() {
  if (!safeStorage.isEncryptionAvailable() || (process.platform === 'linux'
      && !['gnome_libsecret', 'kwallet', 'kwallet5', 'kwallet6'].includes(safeStorage.getSelectedStorageBackend?.()))) {
    throw new Error('系统凭据加密不可用，请启用系统密钥环，或每次手动输入 SSH 密码');
  }
}

function listSshHosts() {
  return publicHosts(readHosts());
}

function saveSshHost(data) {
  const target = sshPasswordTarget(data);
  assertSshPassword(data.password);
  requireCredentialStorage();
  let encryptedPassword;
  try {
    encryptedPassword = safeStorage.encryptString(data.password).toString('base64');
  } catch {
    throw new Error('SSH 密码加密失败，请检查系统凭据存储');
  }
  const hosts = readHosts();
  hosts.set(hostId(target), { ...target, encryptedPassword });
  writeHosts(hosts);
  return publicHosts(hosts);
}

function removeSshHost(id) {
  const hosts = readHosts();
  if (hosts.delete(id)) writeHosts(hosts);
  return publicHosts(hosts);
}

function savedSshPassword(data) {
  const host = readHosts().get(hostId(sshPasswordTarget(data)));
  if (!host) throw new Error('请输入 SSH 密码，或选择已保存的主机');
  requireCredentialStorage();
  try {
    return safeStorage.decryptString(Buffer.from(host.encryptedPassword, 'base64'));
  } catch {
    throw new Error('无法解密已保存的 SSH 密码，请重新输入密码并保存主机');
  }
}

module.exports = { listSshHosts, saveSshHost, removeSshHost, savedSshPassword };
