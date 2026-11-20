const JAVA_CLASS = /^[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*$/;
const PROXY_CLASS = /\$\$(?:EnhancerBy(?:Spring)?CGLIB|FastClassBy(?:Spring)?CGLIB|SpringCGLIB|FastClass)\$\$|^(?:(?:com\.sun\.proxy|jdk\.proxy\d+)\.)?\$Proxy\d+$/;
const SPRING_MAPPING = /^org\.springframework\.web\.bind\.annotation\.(Request|Get|Post|Put|Delete|Patch)Mapping$/;
const ACCESS_LEVELS = ['public', 'protected', 'private'];
const TYPES_WITHOUT_CONTRACTS = new Set([
  'java.lang.Object', 'java.io.Serializable', 'java.lang.Cloneable', 'java.util.RandomAccess',
  'org.springframework.aop.SpringProxy'
]);
const MAX_METADATA_TYPES = 32;
const MAX_METADATA_ROUNDS = 4;

function methodRows(rows) {
  return rows.filter((row) => row.type === 'sm' && row.methodInfo?.methodName && row.methodInfo.constructor !== true
    && row.methodInfo.methodName !== '<init>')
    .map((row) => row.methodInfo);
}

function hasModifier(info, modifier) {
  return info.modifier?.split(/[,\s]+/).includes(modifier);
}

function isInterface(type) {
  return type.interface === true || hasModifier(type, 'interface');
}

function typePattern(names) {
  // Arthas 仍可能返回子类，使用结果中的声明类再做一次精确匹配。
  return "'^(?:" + names.map((name) => name.replace(/[.$]/g, '[$&]')).join('|') + ")$'";
}

function typeKey(name, loader) {
  return JSON.stringify([name, loader ?? 'null']);
}

function parameterSignature(method) {
  const closing = method.descriptor?.indexOf(')');
  return closing >= 0 ? method.methodName + method.descriptor.slice(0, closing + 1) : undefined;
}

function resolveType(types, name, loader) {
  const candidates = types.get(name) ?? [];
  return candidates.find((type) => type.classLoaderHash === loader)
    ?? (candidates.length === 1 ? candidates[0] : undefined);
}

function lineage(owner, types) {
  const visited = new Set();
  const pending = [owner];
  const ancestors = [];
  let incomplete = false;
  for (const type of pending) {
    const key = typeKey(type.name, type.classLoaderHash);
    if (visited.has(key)) continue;
    visited.add(key);
    ancestors.push(type);
    const names = [...(type.interfaces ?? []), ...(type.superClass ?? [])];
    for (const name of names) {
      if (TYPES_WITHOUT_CONTRACTS.has(name)) continue;
      const parent = resolveType(types, name, type.classLoaderHash);
      if (parent) pending.push(parent);
      else incomplete = true;
    }
  }
  return { ancestors, incomplete };
}

async function describeRuntimeMethods(rows, read, signal) {
  const methods = methodRows(rows);
  const types = new Map();
  const queried = new Set();
  const declarations = new Map();
  let pending = [...new Set(methods.map((method) => method.declaringClass))];
  let warning;
  let incomplete = false;
  const relatedNames = new Set();
  let extra = [];

  try {
    for (let round = 0; pending.length && round < MAX_METADATA_ROUNDS; round++) {
      const names = pending.filter((name) => typeof name === 'string' && JAVA_CLASS.test(name))
        .slice(0, MAX_METADATA_TYPES - queried.size);
      if (!names.length) break;
      const requested = new Set(names);
      names.forEach((name) => queried.add(name));
      const metadata = await read('sc -d -n 100 -E ' + typePattern(names));
      for (const { type, classInfo } of metadata) {
        if (type !== 'sc' || !requested.has(classInfo?.name)) continue;
        const candidates = types.get(classInfo.name) ?? [];
        candidates.push(classInfo);
        types.set(classInfo.name, candidates);
      }
      const related = [...types.values()].flat(1).flatMap((type) => [...(type.interfaces ?? []), ...(type.superClass ?? [])]);
      pending = [...new Set([...pending, ...related])]
        .filter((name) => !TYPES_WITHOUT_CONTRACTS.has(name) && !queried.has(name));
    }

    for (const [name, candidates] of types) {
      if (candidates.some(isInterface)) relatedNames.add(name);
      if (PROXY_CLASS.test(name)) {
        for (const type of candidates) {
          const base = type.superClass?.[0];
          if (types.has(base) && base !== 'java.lang.reflect.Proxy') relatedNames.add(base);
        }
      }
    }
    // 复用第一次 sm 返回的方法，仅为缺失的接口或代理父类补查声明。
    const knownOwners = new Set(rows.filter((row) => row.type === 'sm' && row.methodInfo).map(({ methodInfo }) =>
      typeKey(methodInfo.declaringClass, methodInfo.classLoaderHash)));
    const missing = [...relatedNames].filter((name) => types.get(name).some((candidate) =>
      !knownOwners.has(typeKey(name, candidate.classLoaderHash))));
    if (missing.length) extra = methodRows(await read('sm -d -n 100 -E ' + typePattern(missing)));
  } catch (error) {
    if (signal.aborted) throw error;
    warning = error.message;
  }
  for (const method of [...methods, ...extra]) {
    const signature = parameterSignature(method);
    if (!signature || !relatedNames.has(method.declaringClass) || hasModifier(method, 'static')) continue;
    const key = typeKey(method.declaringClass, method.classLoaderHash) + signature;
    const matches = declarations.get(key) ?? [];
    matches.push(method);
    declarations.set(key, matches);
  }

  const hierarchies = new Map();
  const classified = methods.map((method) => {
    const { declaringClass, classLoaderHash, methodName, modifier } = method;
    const key = typeKey(declaringClass, classLoaderHash);
    if (!hierarchies.has(key)) {
      const owner = types.get(declaringClass)?.find((item) => item.classLoaderHash === classLoaderHash);
      const hierarchy = owner ? lineage(owner, types) : { ancestors: [], incomplete: true };
      incomplete ||= hierarchy.incomplete;
      hierarchies.set(key, hierarchy.ancestors);
    }
    const access = ACCESS_LEVELS.find((level) => hasModifier(method, level)) ?? (typeof modifier === 'string' ? 'package' : 'unknown');
    const proxy = PROXY_CLASS.test(declaringClass) || methodName.startsWith('CGLIB$');
    const annotations = new Set(method.annotations ?? []);
    const interfaces = [];
    const signature = parameterSignature(method);
    if (signature && !hasModifier(method, 'static')) {
      for (const type of hierarchies.get(key)) {
        const contract = isInterface(type);
        const matches = declarations.get(typeKey(type.name, type.classLoaderHash) + signature) ?? [];
        if (contract && access === 'public' && matches.some((match) => hasModifier(match, 'public'))) {
          interfaces.push(type.name);
        }
        if ((contract && access === 'public') || (proxy && type.name !== declaringClass && access !== 'private')) {
          for (const match of matches) {
            if (!hasModifier(match, 'private')) match.annotations?.forEach((annotation) => annotations.add(annotation));
          }
        }
      }
    }
    return { ...method, access, proxy, interfaces, httpMappings: [...annotations].filter((item) => SPRING_MAPPING.test(item)) };
  });

  return { rows, methods: classified, warning: warning || (incomplete && '部分类型的声明或 ClassLoader 未能确定') || '' };
}

module.exports = { JAVA_CLASS, describeRuntimeMethods };
