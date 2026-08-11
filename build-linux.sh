#!/usr/bin/env bash
set -euo pipefail

[[ "$(uname -s)" == "Linux" ]] || { echo "该脚本只能在 Linux 上执行。" >&2; exit 1; }
app_root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
frontend_root="$app_root/frontend"
backend_resource="$frontend_root/backend"
runtime_resource="$frontend_root/runtime"
frontend_dist="$frontend_root/dist"
release_root="$app_root/release/linux"

command -v java >/dev/null 2>&1 || { echo "未找到 JDK 17 或更高版本。" >&2; exit 1; }
command -v jlink >/dev/null 2>&1 || { echo "当前 JAVA_HOME 不是完整 JDK，缺少 jlink。" >&2; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "未找到 Maven。" >&2; exit 1; }
command -v npm >/dev/null 2>&1 || { echo "未找到 Node.js 和 npm。" >&2; exit 1; }

required_node_version="$(tr -d '\r\n' < "$app_root/.node-version")"
actual_node_version="$(node --version)"
actual_node_version="${actual_node_version#v}"
required_npm_version="$(node -p "require('$app_root/package.json').engines.npm")"
actual_npm_version="$(npm --version)"
[[ "$actual_node_version" == "$required_node_version" && "$actual_npm_version" == "$required_npm_version" ]] || {
  echo "Node.js/npm 版本必须为 $required_node_version/$required_npm_version，当前为 $actual_node_version/$actual_npm_version" >&2
  exit 1
}

java_major="$(java -version 2>&1 | sed -nE '1s/.*version "([0-9]+).*/\1/p')"
[[ -n "$java_major" && "$java_major" -ge 17 ]] || { echo "后端构建至少需要 JDK 17。" >&2; exit 1; }

(cd "$app_root" && mvn "-Dmaven.test.skip=true" package)
npm --prefix "$frontend_root" ci

for target in "$backend_resource" "$runtime_resource" "$frontend_dist"; do
  case "$target" in
    "$frontend_root"/*) rm -rf -- "$target" ;;
    *) echo "拒绝清理前端目录之外的路径：$target" >&2; exit 1 ;;
  esac
done
mkdir -p "$backend_resource"
cp "$app_root/backend/target/jarpatch-studio-backend.jar" "$backend_resource/jarpatch-studio-backend.jar"

runtime_modules="java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.security.jgss,java.sql,java.transaction.xa,java.xml,jdk.crypto.ec,jdk.unsupported,jdk.zipfs"
jlink --add-modules "$runtime_modules" --strip-debug --no-header-files --no-man-pages --compress=2 --output "$runtime_resource"
npm --prefix "$frontend_root" run dist:linux

case "$release_root" in
  "$app_root/release/linux") rm -rf -- "$release_root" ;;
  *) echo "拒绝清理非预期发布目录：$release_root" >&2; exit 1 ;;
esac
mkdir -p "$release_root"
shopt -s nullglob
artifacts=("$frontend_root"/dist/*.AppImage "$frontend_root"/dist/*.tar.gz)
[[ "${#artifacts[@]}" -gt 0 ]] || { echo "未生成 Linux 发布产物。" >&2; exit 1; }
cp -- "${artifacts[@]}" "$release_root/"
case "$(uname -m)" in
  x86_64) release_architecture="x64" ;;
  arm64|aarch64) release_architecture="arm64" ;;
  *) echo "不支持的 Linux 架构：$(uname -m)" >&2; exit 1 ;;
esac
java_version="$(java -version 2>&1 | head -n 1)"
(cd "$app_root" && node scripts/generate-release-manifest.js linux "$release_architecture" "$release_root" "$java_version" "$actual_npm_version")
echo "Linux 发布包已生成：$release_root"
