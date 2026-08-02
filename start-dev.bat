@echo off
chcp 65001 >nul
title 提示词工程实战训练系统 - 开发启动器
echo ==========================================
echo   提示词工程实战训练系统 - 开发启动器
echo ==========================================
cd /d "%~dp0"

REM 定位 Maven：优先 PATH 中的 mvn，其次项目 mvnw.cmd
set "MAVEN_CMD=mvn"
where mvn >nul 2>nul
if %errorlevel% neq 0 (
  if exist "mvnw.cmd" (
    set "MAVEN_CMD=mvnw.cmd"
  ) else (
    echo [错误] 未找到 Maven，请配置 PATH 或使用项目 mvnw
    pause
    exit /b 1
  )
)

echo.
echo [1/2] 启动后端 Spring Boot ^(http://localhost:8080^) ...
start "后端 - Spring Boot :8080" cmd /k "%MAVEN_CMD% spring-boot:run"

echo [2/2] 启动前端 Vite ^(http://localhost:5173^) ...
start "前端 - Vite :5173" cmd /k "cd /d web && pnpm dev"

echo.
echo 启动完成！稍等片刻后访问：
echo   综合入口  http://localhost:5173/
echo   用户端    http://localhost:5173/chat
echo   管理后台  http://localhost:5173/admin
echo.
pause
