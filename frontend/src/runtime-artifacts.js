const { randomBytes } = require('crypto');
const { setTimeout: delay } = require('timers/promises');
const { sha256 } = require('./runtime-validation');

const MAX_CLASS_BYTES = 1024 * 1024;
const MAX_PROFILE_BYTES = 24 * 1024 * 1024;
const CLASS_MAGIC = 0xcafebabe;
const PROFILE_TYPES = {
  jfr: { extension: 'jfr', label: 'JFR' },
  flame: { extension: 'html', label: '火焰图' }
};
const PROFILER_EVENTS = new Set(['itimer', 'cpu', 'wall']);

function requireUniqueDump(rows, directory, windows) {
  const classes = rows.flatMap((item) => Array.isArray(item.dumpedClasses) ? item.dumpedClasses : []);
  if (!classes.length) throw new Error('目标类未加载或无法导出');
  if (classes.length !== 1) throw new Error('有多个同名类，请先选择 ClassLoader 哈希');
  const raw = classes[0].location;
  const location = windows && typeof raw === 'string' ? raw.replace(/\\/g, '/') : raw;
  if (typeof location !== 'string' || !location.startsWith(directory + '/') || !location.endsWith('.class')
      || location.split('/').includes('..') || /[\u0000-\u001f]/.test(location)) {
    throw new Error('Arthas 返回的 class 路径超出本次任务目录');
  }
  return location;
}

function artifactArgument(location) {
  return /[\s"'\\]/.test(location) ? JSON.stringify(location) : location;
}

async function collectClass(access, session, className, loader, signal) {
  const directory = session.directory + '/class-' + randomBytes(12).toString('hex');
  await access.create(directory, signal);
  try {
    const rows = await access.exec('dump -d ' + artifactArgument(directory) + (loader ? ' -c ' + loader : '') + ' ' + className, signal);
    const location = requireUniqueDump(rows, directory, /^[a-z]:\//i.test(directory) || directory.startsWith('//'));
    const contents = await access.read(location, MAX_CLASS_BYTES, signal);
    if (contents.length < 8 || contents.readUInt32BE() !== CLASS_MAGIC) throw new Error('运行中 class 格式无效');
    return { className, loader, sha256: sha256(contents), size: contents.length, contents: contents.toString('base64') };
  } finally {
    await access.remove(directory);
  }
}

function profileText(rows) {
  return rows.flatMap((row) => [row.executeResult, row.jfrOutput, row.message, row.result])
    .filter((value) => typeof value === 'string').join('\n');
}

async function collectProfile(access, session, type, seconds, event, signal, emit) {
  const definition = Object.hasOwn(PROFILE_TYPES, type) ? PROFILE_TYPES[type] : null;
  if (!definition || !Number.isInteger(seconds) || seconds < 5 || seconds > 30) throw new Error('采集时间须为 5 至 30 秒');
  if (type === 'flame' && !PROFILER_EVENTS.has(event)) throw new Error('火焰图采样事件无效');
  const name = 'jarpatch-' + randomBytes(12).toString('hex');
  const directory = session.directory + '/profile-' + name.slice('jarpatch-'.length);
  const location = directory + '/recording.' + definition.extension;
  await access.create(directory, signal);
  try {
    if (type === 'flame') {
      const status = await access.exec('profiler status', signal);
      if (!/not active|not running|inactive/i.test(profileText(status))) throw new Error('当前 JVM 已在采样或采样状态不可确认');
    }
    const command = type === 'jfr'
      ? 'jfr start -n ' + name + ' --duration ' + seconds + 's --maxsize 16m -f ' + artifactArgument(location)
      : 'profiler start --event ' + event + ' --timeout ' + seconds + ' --file ' + artifactArgument(location) + ' --format flamegraph';
    if (signal.aborted) throw new Error('操作已取消');
    // 启动命令交给 JVM 自行限时。取消或断开时等到该次采集截止，避免停止别人的采样。
    let startError;
    try {
      const rows = await access.exec(command);
      const result = profileText(rows);
      if (/not supported|failed|unknown|exception|error|unsupported/i.test(result)) throw new Error(result.trim().slice(0, 400));
    } catch (error) {
      startError = error;
    }
    emit('status', '等待 ' + definition.label + ' 采集结束（' + seconds + ' 秒）');
    await delay((seconds + 1) * 1000);
    if (startError) throw startError;
    if (signal.aborted) throw new Error('操作已取消');
    let contents;
    for (let attempt = 0; attempt < 5; attempt++) {
      try {
        contents = await access.read(location, MAX_PROFILE_BYTES, signal);
        break;
      } catch (error) {
        const pending = ['ENOENT', 'JARPATCH_ARTIFACT_PENDING'].includes(error.code) || /^远端操作退出码 197\b/.test(error.message);
        if (attempt === 4 && type === 'jfr' && error.code === 'JARPATCH_ARTIFACT_PENDING') {
          throw new Error('JFR 录制结束后文件仍为空，请检查目标 JVM 的 JFR 日志和临时目录');
        }
        if (attempt === 4 || signal.aborted || !pending) throw error;
        await delay(400, undefined, { signal });
      }
    }
    if (type === 'jfr' && contents.subarray(0, 4).toString('hex') !== '464c5200') throw new Error('JVM 未生成有效的 JFR 文件');
    if (type === 'flame' && !/<html|<!doctype/i.test(contents.subarray(0, 2048).toString('utf8'))) {
      throw new Error('采样器未生成有效的火焰图');
    }
    return { type, filename: name + '.' + definition.extension, label: definition.label,
      duration: seconds, event: type === 'flame' ? event : undefined, contents };
  } finally {
    await access.remove(directory);
  }
}

module.exports = { collectClass, collectProfile };
