const fs = require('fs');
const path = require('path');

const MAX_LOG_BYTES = 1024 * 1024;
const REDACTED = '[REDACTED]';

/**
 * 创建独立于 Java 后端的桌面启动日志，保留一个轮换备份；日志写入失败会报告到 stderr。
 * @param {string} userDataDirectory 当前 Electron 用户数据目录
 * @returns {{filePath: string, protectSecret: function, write: function}} 启动诊断记录器
 */
function createStartupDiagnostics(userDataDirectory) {
  const filePath = process.env.JARPATCH_STARTUP_LOG
    || path.join(userDataDirectory, 'logs', 'startup.log');
  const secrets = [];
  return {
    filePath,
    protectSecret(value) {
      if (value) secrets.push(value);
    },
    write(event, detail = '') {
      try {
        fs.mkdirSync(path.dirname(filePath), { recursive: true });
        if (fs.existsSync(filePath) && fs.statSync(filePath).size > MAX_LOG_BYTES) {
          const previous = `${filePath}.previous`;
          if (fs.existsSync(previous)) fs.unlinkSync(previous);
          fs.renameSync(filePath, previous);
        }
        let text = JSON.stringify({ time: new Date().toISOString(), pid: process.pid, event, detail });
        for (const secret of secrets) text = text.split(secret).join(REDACTED);
        text = text.replace(/(token|password|secret|api[_-]?key)([\\"\s]*[:=][\\"\s]*)[^\s,;"}]+/gi,
          `$1$2${REDACTED}`);
        fs.appendFileSync(filePath, `${text}\n`, 'utf8');
      } catch (error) {
        process.stderr.write(`[STARTUP_LOG_FAILURE] ${error.message}\n`);
      }
    }
  };
}

module.exports = { createStartupDiagnostics };
