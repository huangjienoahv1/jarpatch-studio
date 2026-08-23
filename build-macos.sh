#!/usr/bin/env bash
set -euo pipefail

[[ "$(uname -s)" == "Darwin" ]] || { echo "该脚本只能在 macOS 上执行。" >&2; exit 1; }
app_root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
frontend_root="$app_root/frontend"
backend_resource="$frontend_root/backend"
runtime_resource="$frontend_root/runtime"
frontend_dist="$frontend_root/dist"
release_root="$app_root/release/macos"
build_entry="build-macos.sh"
require_signing="${JARPATCH_REQUIRE_SIGNING:-false}"
signing_identity="${JARPATCH_MAC_SIGNING_IDENTITY:-}"
notary_profile="${JARPATCH_MAC_NOTARY_PROFILE:-}"

command -v git >/dev/null 2>&1 || { echo "未找到 Git，无法记录发布源码状态。" >&2; exit 1; }
command -v shasum >/dev/null 2>&1 || { echo "未找到 shasum，无法校验构建入口。" >&2; exit 1; }
command -v java >/dev/null 2>&1 || { echo "未找到 JDK 17 或更高版本。" >&2; exit 1; }
command -v jlink >/dev/null 2>&1 || { echo "当前 JAVA_HOME 不是完整 JDK，缺少 jlink。" >&2; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "未找到 Maven。" >&2; exit 1; }
command -v npm >/dev/null 2>&1 || { echo "未找到 Node.js 和 npm。" >&2; exit 1; }
command -v node >/dev/null 2>&1 || { echo "未找到 Node.js。" >&2; exit 1; }

if [[ "$require_signing" == "true" ]]; then
  [[ -n "$signing_identity" ]] || { echo "正式发布要求签名，但未设置 JARPATCH_MAC_SIGNING_IDENTITY。" >&2; exit 1; }
  [[ -n "$notary_profile" ]] || { echo "正式发布要求公证，但未设置 JARPATCH_MAC_NOTARY_PROFILE。" >&2; exit 1; }
  command -v codesign >/dev/null 2>&1 || { echo "未找到 codesign。" >&2; exit 1; }
  command -v xcrun >/dev/null 2>&1 || { echo "未找到 xcrun。" >&2; exit 1; }
  export CSC_NAME="$signing_identity"
fi

node "$app_root/scripts/sync-version.js" --check

git_commit="$(git -C "$app_root" rev-parse HEAD)"
if [[ -z "$(git -C "$app_root" status --porcelain --untracked-files=all)" ]]; then
  source_clean="true"
else
  source_clean="false"
fi
build_entry_sha256="$(shasum -a 256 "$app_root/$build_entry" | awk '{print $1}')"

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
npm --prefix "$frontend_root" run dist:mac

case "$(uname -m)" in
  x86_64) release_architecture="x64"; unpacked_root="$frontend_root/dist/mac" ;;
  arm64|aarch64) release_architecture="arm64"; unpacked_root="$frontend_root/dist/mac-arm64" ;;
  *) echo "不支持的 macOS 架构：$(uname -m)" >&2; exit 1 ;;
esac
smoke_log="$(mktemp)"
"$unpacked_root/JarPatch Studio.app/Contents/MacOS/JarPatch Studio" --smoke-check >"$smoke_log" 2>&1 &
smoke_pid=$!
for _ in $(seq 1 60); do
  kill -0 "$smoke_pid" 2>/dev/null || break
  sleep 1
done
if kill -0 "$smoke_pid" 2>/dev/null; then
  kill "$smoke_pid" 2>/dev/null || true
  wait "$smoke_pid" 2>/dev/null || true
  echo "macOS 解包应用启动验收超时。" >&2
  rm -f -- "$smoke_log"
  exit 1
fi
smoke_exit=0
wait "$smoke_pid" || smoke_exit=$?
smoke_output="$(cat "$smoke_log")"
rm -f -- "$smoke_log"
[[ "$smoke_exit" -eq 0 && "$smoke_output" == *'"status":"READY"'* ]] || {
  echo "macOS 解包应用启动验收失败：$smoke_output" >&2
  exit 1
}
if [[ "$require_signing" == "true" ]]; then
  codesign --verify --deep --strict --verbose=2 "$unpacked_root/JarPatch Studio.app"
fi

case "$release_root" in
  "$app_root/release/macos") rm -rf -- "$release_root" ;;
  *) echo "拒绝清理非预期发布目录：$release_root" >&2; exit 1 ;;
esac
mkdir -p "$release_root"
shopt -s nullglob
artifacts=("$frontend_root"/dist/*.dmg "$frontend_root"/dist/*.zip)
[[ "${#artifacts[@]}" -gt 0 ]] || { echo "未生成 macOS 发布产物。" >&2; exit 1; }
if [[ "$require_signing" == "true" ]]; then
  dmg_artifacts=("$frontend_root"/dist/*.dmg)
  [[ "${#dmg_artifacts[@]}" -gt 0 ]] || { echo "未生成可公证的 DMG。" >&2; exit 1; }
  for dmg in "${dmg_artifacts[@]}"; do
    xcrun notarytool submit "$dmg" --keychain-profile "$notary_profile" --wait
    xcrun stapler staple "$dmg"
    xcrun stapler validate "$dmg"
  done
fi
cp -- "${artifacts[@]}" "$release_root/"
java_version="$(java -version 2>&1 | head -n 1)"
if [[ "$require_signing" == "true" ]]; then signing_status="VALID_NOTARIZED"; else signing_status="NOT_SIGNED"; fi
(cd "$app_root" && node scripts/generate-release-manifest.js macos "$release_architecture" "$release_root" "$java_version" "$actual_npm_version" "$git_commit" "$source_clean" "$build_entry" "$build_entry_sha256" "$signing_status")
echo "macOS 发布包已生成：$release_root"
