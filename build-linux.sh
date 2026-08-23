#!/usr/bin/env bash
set -euo pipefail

[[ "$(uname -s)" == "Linux" ]] || { echo "该脚本只能在 Linux 上执行。" >&2; exit 1; }
app_root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
frontend_root="$app_root/frontend"
backend_resource="$frontend_root/backend"
runtime_resource="$frontend_root/runtime"
frontend_dist="$frontend_root/dist"
release_root="$app_root/release/linux"
build_entry="build-linux.sh"
require_signing="${JARPATCH_REQUIRE_SIGNING:-false}"
gpg_key_id="${JARPATCH_LINUX_GPG_KEY_ID:-}"

command -v git >/dev/null 2>&1 || { echo "未找到 Git，无法记录发布源码状态。" >&2; exit 1; }
command -v sha256sum >/dev/null 2>&1 || { echo "未找到 sha256sum，无法校验构建入口。" >&2; exit 1; }
command -v java >/dev/null 2>&1 || { echo "未找到 JDK 17 或更高版本。" >&2; exit 1; }
command -v jlink >/dev/null 2>&1 || { echo "当前 JAVA_HOME 不是完整 JDK，缺少 jlink。" >&2; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "未找到 Maven。" >&2; exit 1; }
command -v npm >/dev/null 2>&1 || { echo "未找到 Node.js 和 npm。" >&2; exit 1; }
command -v node >/dev/null 2>&1 || { echo "未找到 Node.js。" >&2; exit 1; }
command -v timeout >/dev/null 2>&1 || { echo "未找到 timeout，无法执行原生启动验收。" >&2; exit 1; }
if [[ "$require_signing" == "true" ]]; then
  [[ -n "$gpg_key_id" ]] || { echo "正式发布要求签名，但未设置 JARPATCH_LINUX_GPG_KEY_ID。" >&2; exit 1; }
  command -v gpg >/dev/null 2>&1 || { echo "未找到 gpg。" >&2; exit 1; }
fi

node "$app_root/scripts/sync-version.js" --check

git_commit="$(git -C "$app_root" rev-parse HEAD)"
if [[ -z "$(git -C "$app_root" status --porcelain --untracked-files=all)" ]]; then
  source_clean="true"
else
  source_clean="false"
fi
build_entry_sha256="$(sha256sum "$app_root/$build_entry" | awk '{print $1}')"

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

case "$(uname -m)" in
  x86_64) release_architecture="x64"; unpacked_root="$frontend_root/dist/linux-unpacked" ;;
  arm64|aarch64) release_architecture="arm64"; unpacked_root="$frontend_root/dist/linux-arm64-unpacked" ;;
  *) echo "不支持的 Linux 架构：$(uname -m)" >&2; exit 1 ;;
esac
smoke_output="$(timeout 60 "$unpacked_root/jarpatch-studio" --smoke-check 2>&1)" || {
  echo "Linux 解包应用启动验收失败：$smoke_output" >&2
  exit 1
}
grep -F '"status":"READY"' <<< "$smoke_output" >/dev/null || {
  echo "Linux 解包应用未返回 READY：$smoke_output" >&2
  exit 1
}

case "$release_root" in
  "$app_root/release/linux") rm -rf -- "$release_root" ;;
  *) echo "拒绝清理非预期发布目录：$release_root" >&2; exit 1 ;;
esac
mkdir -p "$release_root"
shopt -s nullglob
artifacts=("$frontend_root"/dist/*.AppImage "$frontend_root"/dist/*.tar.gz)
[[ "${#artifacts[@]}" -gt 0 ]] || { echo "未生成 Linux 发布产物。" >&2; exit 1; }
cp -- "${artifacts[@]}" "$release_root/"
if [[ "$require_signing" == "true" ]]; then
  for artifact in "$release_root"/*.AppImage "$release_root"/*.tar.gz; do
    gpg --batch --yes --local-user "$gpg_key_id" --armor --detach-sign "$artifact"
    gpg --verify "$artifact.asc" "$artifact"
  done
fi
java_version="$(java -version 2>&1 | head -n 1)"
if [[ "$require_signing" == "true" ]]; then signing_status="VALID_GPG"; else signing_status="NOT_SIGNED"; fi
(cd "$app_root" && node scripts/generate-release-manifest.js linux "$release_architecture" "$release_root" "$java_version" "$actual_npm_version" "$git_commit" "$source_clean" "$build_entry" "$build_entry_sha256" "$signing_status")
echo "Linux 发布包已生成：$release_root"
