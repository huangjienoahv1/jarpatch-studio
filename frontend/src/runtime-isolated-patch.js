const fs = require('fs/promises');
const { randomBytes } = require('crypto');
const { sha256 } = require('./runtime-validation');

const MAX_BYTES = 1024 * 1024;
const FIXED_POOL_LENGTHS = { 3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4, 10: 4, 11: 4,
  12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 2 };

// Inspect this_class without loading the supplied code on the desktop or trusting its filename.
function binaryClassName(bytes) {
  if (bytes.length < 10 || bytes.readUInt32BE(0) !== 0xcafebabe) throw new Error('请选择有效的 Java class 文件');
  const count = bytes.readUInt16BE(8);
  const utf8 = new Map();
  const classes = new Map();
  let offset = 10;
  try {
    for (let index = 1; index < count; index++) {
      const tag = bytes.readUInt8(offset++);
      if (tag === 1) {
        const length = bytes.readUInt16BE(offset);
        offset += 2;
        if (offset + length > bytes.length) throw new RangeError('truncated');
        utf8.set(index, bytes.toString('utf8', offset, offset + length));
        offset += length;
      } else {
        const width = FIXED_POOL_LENGTHS[tag];
        if (!width) throw new RangeError('tag');
        if (tag === 7) classes.set(index, bytes.readUInt16BE(offset));
        if (tag === 5 || tag === 6) index++;
        offset += width;
      }
    }
    const name = utf8.get(classes.get(bytes.readUInt16BE(offset + 2)));
    if (!name) throw new RangeError('this_class');
    return name.replace(/\//g, '.');
  } catch {
    throw new Error('无法读取 class 的内部类名');
  }
}

function validatePatch(bytes, expectedClass) {
  if (bytes.length > MAX_BYTES) throw new Error('class 文件大小超过 1 MiB');
  if (binaryClassName(bytes) !== expectedClass) throw new Error('class 的内部类名与目标不一致');
  return bytes;
}

async function readPatch(file, expectedClass, signal) {
  if (!file || !file.toLowerCase().endsWith('.class')) throw new Error('请从文件选择器选择 class');
  const handle = await fs.open(file, 'r');
  try {
    const info = await handle.stat();
    if (!info.isFile() || info.size > MAX_BYTES) throw new Error('class 文件大小超过 1 MiB');
    const buffer = Buffer.alloc(MAX_BYTES + 1);
    const { bytesRead } = await handle.read(buffer, 0, buffer.length, 0);
    if (signal.aborted) throw new Error('操作已取消');
    return validatePatch(buffer.subarray(0, bytesRead), expectedClass);
  } finally {
    await handle.close();
  }
}

function patchDirectory(session) {
  return session.directory + '/hotpatch-' + randomBytes(12).toString('hex');
}

function patchResult(state, className, bytes) {
  return { state, className, sha256: sha256(bytes) };
}

module.exports = { readPatch, validatePatch, patchDirectory, patchResult };
