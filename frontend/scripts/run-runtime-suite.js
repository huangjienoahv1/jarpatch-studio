const fs = require('fs/promises');
const path = require('path');
const { RuntimeWorkbench } = require('../src/runtime-workbench');
const { MAX_JSON_BYTES, normalizeSuite } = require('../src/runtime-validation');

async function readJson(file) {
  const handle = await fs.open(file, 'r');
  try {
    const data = Buffer.alloc(MAX_JSON_BYTES + 1);
    const { bytesRead } = await handle.read(data, 0, data.length, 0);
    if (bytesRead > MAX_JSON_BYTES) throw new Error('配置或用例文件超过 512 KiB');
    return JSON.parse(data.subarray(0, bytesRead).toString('utf8').replace(/^\uFEFF/, ''));
  } finally {
    await handle.close();
  }
}

function isPassed(result) {
  return !result.error && result.assertions.every((rule) => rule.passed);
}

function toJUnit(run) {
  const escape = (value) => String(value).replace(/[\u0000-\u0008\u000b\u000c\u000e-\u001f]/g, '')
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&apos;');
  const failed = run.results.filter((test) => !isPassed(test)).length;
  const rows = run.results.map((test) => {
    const testCase = '<testcase classname="JarPatch.Runtime" name="' + escape(test.name) + '" time="' + ((test.elapsedMs ?? 0) / 1000) + '">';
    if (isPassed(test)) return testCase + '</testcase>';
    const message = test.error ? '请求失败' : '断言失败：' + test.assertions.filter((item) => !item.passed).map((item) => item.type).join(', ');
    return testCase + '<failure message="' + escape(message) + '" /></testcase>';
  });
  return '<?xml version="1.0" encoding="UTF-8"?>\n<testsuite name="JarPatch.Runtime" tests="'
    + run.results.length + '" failures="' + failed + '">\n' + rows.join('\n') + '\n</testsuite>\n';
}

async function main() {
  const [, , file, format = 'json'] = process.argv;
  if (!file || !['json', 'junit'].includes(format) || process.argv.length > 4) {
    throw new Error('用法：node frontend/scripts/run-runtime-suite.js 配置.json [json|junit]');
  }
  const config = await readJson(file);
  if (!config || typeof config.suite !== 'string' || typeof config.target !== 'string' || !config.target
      || typeof config.host !== 'string' || !Number.isInteger(config.port) || config.port < 1 || config.port > 65535) {
    throw new Error('运行配置须提供 host、target、port 和 suite');
  }
  if (config.pid !== undefined && (!/^\d+$/.test(String(config.pid)) || Number(config.pid) < 1)) {
    throw new Error('配置的 JVM PID 无效');
  }
  const suite = normalizeSuite(await readJson(path.resolve(path.dirname(file), config.suite)));
  const bench = new RuntimeWorkbench();
  const run = (id, fields) => bench.run(id, { host: config.host, ...fields });
  let activeId;
  let interrupted = false;
  const interrupt = () => { interrupted = true; if (activeId) bench.cancel(activeId); };
  process.once('SIGINT', interrupt);
  process.once('SIGTERM', interrupt);
  try {
    async function step(id, fields) {
      if (interrupted) throw new Error('操作已取消');
      activeId = id;
      return run(id, fields);
    }
    const items = await step('list', { action: 'containers', cluster: config.cluster });
    const target = items.find((item) => item.id === config.target || item.name === config.target);
    if (!target) throw new Error('目标实例不存在或已停止');
    const processes = await step('jvm', { action: 'processes', container: target.id });
    const selected = config.pid === undefined && processes.length === 1 ? processes[0]
      : processes.find((item) => item.pid === String(config.pid));
    if (!selected) throw new Error('未找到指定 JVM，或存在多个 JVM，请在配置中指定 PID');
    const result = await step('suite', { action: 'suite', suite, container: target.id,
      pid: selected.pid, start: selected.start, port: config.port, credentials: process.env.JARPATCH_RUNTIME_HEADERS });
    const failed = result.results.some((item) => !isPassed(item));
    const clean = { ...result, results: result.results.map((item) => ({ ...item, error: item.error ? '请求失败' : undefined })) };
    process.stdout.write(format === 'junit' ? toJUnit(clean) : JSON.stringify(clean, null, 2) + '\n');
    if (failed) process.exitCode = 1;
  } finally {
    process.removeListener('SIGINT', interrupt);
    process.removeListener('SIGTERM', interrupt);
    await bench.shutdown();
  }
}

main().catch((error) => {
  process.stderr.write(error.message + '\n');
  process.exitCode = 2;
});
