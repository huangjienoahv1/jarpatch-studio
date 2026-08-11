#!/usr/bin/env bash
set -euo pipefail

app_root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
backend_jar="$app_root/backend/target/jarpatch-studio-backend.jar"

command -v java >/dev/null 2>&1 || { echo "未找到 Java 17 或更高版本。" >&2; exit 1; }
command -v npm >/dev/null 2>&1 || { echo "未找到 Node.js 和 npm。" >&2; exit 1; }

if [[ ! -f "$backend_jar" ]]; then
  command -v mvn >/dev/null 2>&1 || { echo "未找到 Maven，且后端包尚未构建。" >&2; exit 1; }
  (cd "$app_root" && mvn "-Dmaven.test.skip=true" package)
fi

if [[ ! -d "$app_root/frontend/node_modules/electron" ]]; then
  npm --prefix "$app_root/frontend" ci
fi

npm --prefix "$app_root/frontend" start
