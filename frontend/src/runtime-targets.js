const { createHash } = require('crypto');
const { runSsh, shellCommand, dockerExec } = require('./runtime-transport');

const RESOURCE = /^[a-z0-9](?:[a-z0-9.-]{0,251}[a-z0-9])?$/;
const CONTEXT = /^[A-Za-z0-9][A-Za-z0-9_@.:/-]{0,199}$/;
const MAX_TARGETS = 2000;

function requireName(name, label) {
  if (typeof name !== 'string' || !RESOURCE.test(name)) throw new Error(label + '无效');
  return name;
}

function clusterOptions(value) {
  const context = value?.context || '';
  if (context && !CONTEXT.test(context)) throw new Error('Kubernetes Context 名称无效');
  const namespace = requireName(value?.namespace || 'default', '命名空间');
  const debug = value?.debug ? requireName(value.debug, '调试容器名') : '';
  const selector = value?.selector || '';
  const kubeconfig = value?.kubeconfig || '';
  if (typeof selector !== 'string' || selector.length > 250 || /[\u0000-\u001f]/.test(selector)) {
    throw new Error('Pod 标签选择器无效');
  }
  if (typeof kubeconfig !== 'string' || (kubeconfig && !/^\/[^\u0000-\u001f]{1,511}$/.test(kubeconfig))) {
    throw new Error('请填写远程 Linux 上的 kubeconfig 绝对路径');
  }
  return { context, namespace, debug, selector, kubeconfig };
}

function kubectl(cluster, args) {
  return ['kubectl', '--request-timeout=12s', ...(cluster.kubeconfig ? ['--kubeconfig', cluster.kubeconfig] : []),
    ...(cluster.context ? ['--context', cluster.context] : []),
    '--namespace', cluster.namespace, ...args];
}

function runningContainers(pod) {
  return [...(pod.status?.containerStatuses ?? []), ...(pod.status?.ephemeralContainerStatuses ?? []),
    ...(pod.status?.initContainerStatuses ?? [])].filter((status) => status.state?.running && status.containerID);
}

function chooseExecutor(pod, application, running, name) {
  if (!name || name === application.name) return application;
  const selected = running.find((container) => container.name === name);
  if (!selected) return application;
  const ephemeral = (pod.spec?.ephemeralContainers ?? []).find((container) => container.name === selected.name);
  if (pod.spec?.shareProcessNamespace || ephemeral?.targetContainerName === application.name) return selected;
  return application;
}

class RuntimeTargets {
  constructor() {
    this.pods = new Map();
  }

  selected(host, id) {
    return this.pods.get(host + '/' + id);
  }

  async listKubernetes(host, input, signal) {
    const cluster = clusterOptions(input);
    const command = ['get', 'pods', '-o', 'json'];
    if (cluster.selector) command.push('--selector', cluster.selector);
    const result = await runSsh(host, shellCommand(kubectl(cluster, command)), { signal });
    const pods = JSON.parse(result).items;
    if (!Array.isArray(pods)) throw new Error('Kubernetes 未返回 Pod 列表');
    const targets = [];
    for (const pod of pods) {
      if (pod.status?.phase !== 'Running' || pod.metadata?.deletionTimestamp || !pod.metadata?.uid) continue;
      const name = requireName(pod.metadata.name, 'Pod 名');
      const containers = runningContainers(pod);
      for (const item of (pod.spec?.containers ?? [])) {
        const application = containers.find((container) => container.name === item.name);
        if (!application) continue;
        const execution = chooseExecutor(pod, application, containers, cluster.debug);
        const id = createHash('sha256').update(JSON.stringify(
          [cluster.kubeconfig, cluster.context, cluster.namespace, pod.metadata.uid, application.containerID, execution.containerID]
        )).digest('hex');
        const key = host + '/' + id;
        const target = {
          cluster, pod: name, uid: pod.metadata.uid, app: requireName(application.name, '容器名'),
          appId: application.containerID, executor: requireName(execution.name, '执行容器名'), executorId: execution.containerID
        };
        this.pods.set(key, target);
        while (this.pods.size > MAX_TARGETS) this.pods.delete(this.pods.keys().next().value);
        const executionStatus = target.app !== target.executor ? ' · ' + target.executor
          : cluster.debug && cluster.debug !== target.app ? ' · 调试容器不可用' : '';
        targets.push({ id, name: name + ' / ' + target.app, image: item.image,
          status: (application.ready ? 'Ready' : 'Running') + executionStatus,
          kind: 'kubernetes', namespace: cluster.namespace, pod: name, app: target.app, executor: target.executor });
      }
    }
    return targets;
  }

  async confirmPod(host, target, signal, requireExecutor = true) {
    const output = await runSsh(host, shellCommand(kubectl(target.cluster, ['get', 'pod', target.pod, '-o', 'json'])), { signal });
    const pod = JSON.parse(output);
    const current = runningContainers(pod);
    if (pod.metadata?.uid !== target.uid || pod.metadata?.deletionTimestamp
        || !current.some((item) => item.name === target.app && item.containerID === target.appId)
        || (requireExecutor && !current.some((item) => item.name === target.executor && item.containerID === target.executorId))) {
      throw new Error('目标 Pod 或容器实例已变化，请重新加载目标');
    }
  }

  async exec(host, id, args, options = {}) {
    const target = this.selected(host, id);
    if (!target) return dockerExec(host, id, args, options);
    await this.confirmPod(host, target, options.signal);
    const input = options.input != null ? ['-i'] : [];
    const guarded = options.user ? ['sh', '-c',
      '[ "$(id -u):$(id -g)" = "$1" ] || exit 199; shift; exec "$@"', 'sh', options.user, ...args] : args;
    const command = kubectl(target.cluster, ['exec', ...input, target.pod, '-c', target.executor, '--', ...guarded])
      .filter((argument) => argument !== '--request-timeout=12s');
    return runSsh(host, shellCommand(command), options);
  }

  async logs(host, id, settings, options = {}) {
    const target = this.selected(host, id);
    if (target) await this.confirmPod(host, target, options.signal, false);
    const args = ['--timestamps', '--tail', String(settings.tail)];
    if (settings.follow) args.push('--follow');
    if (settings.since) args.push(target ? '--since-time=' + settings.since : '--since=' + settings.since);
    if (settings.until && !target) args.push('--until=' + settings.until);
    let command = target ? kubectl({ ...target.cluster }, ['logs', target.pod, '-c', target.app, ...args])
      : ['docker', 'logs', ...args, id];
    if (target && settings.follow) {
      command = command.filter((argument) => argument !== '--request-timeout=12s');
    }
    if (settings.follow) command = ['timeout', String(settings.seconds), ...command];
    let output = await runSsh(host, shellCommand(command) + ' 2>&1', options);
    if (target && settings.until) {
      const end = Date.parse(settings.until);
      output = output.split('\n').filter((line) => {
        const timestamp = Date.parse(line.split(' ', 1)[0]);
        return Number.isFinite(timestamp) && timestamp <= end;
      }).join('\n');
    }
    return output;
  }
}

module.exports = { RuntimeTargets };
