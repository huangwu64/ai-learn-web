#!/usr/bin/env bash
# 提示词工程实战训练系统 - 开发启动器（Git Bash / Linux / macOS）
cd "$(dirname "$0")"

# 定位 Maven：PATH 中的 mvn → 项目 mvnw → 缓存的 wrapper 发行版
MVN=""
if command -v mvn >/dev/null 2>&1; then
  MVN="mvn"
elif [ -x "./mvnw" ]; then
  MVN="./mvnw"
else
  MVN="$(find "$HOME/.m2/wrapper/dists" -path '*/apache-maven-*/bin/mvn' 2>/dev/null | head -1)"
fi

if [ -z "$MVN" ]; then
  echo "[错误] 未找到 Maven，请安装或配置 PATH"
  exit 1
fi

echo "[1/2] 启动后端 Spring Boot (http://localhost:8080)"
"$MVN" spring-boot:run &
BACK_PID=$!

echo "[2/2] 启动前端 Vite (http://localhost:5173)"
(cd web && pnpm dev) &
FRONT_PID=$!

echo "启动完成！访问："
echo "  综合入口  http://localhost:5173/"
echo "  用户端    http://localhost:5173/chat"
echo "  管理后台  http://localhost:5173/admin"
echo "按 Ctrl+C 停止全部"
trap 'kill $BACK_PID $FRONT_PID 2>/dev/null' EXIT
wait
