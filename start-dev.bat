@echo off
rem ==========================================
rem   Prompt Training System - Dev Launcher
rem   NOTE: keep this file ASCII-only. cmd.exe
rem   parses batch files with the system codepage.
rem ==========================================
cd /d "%~dp0"

rem --- Locate Maven: PATH first, then cached wrapper dist ---
where mvn >nul 2>nul && set "MAVEN_CMD=mvn"
if not defined MAVEN_CMD (
  for /f "delims=" %%i in ('dir /s /b "%USERPROFILE%\.m2\wrapper\dists\mvn.cmd" 2^>nul') do set "MAVEN_CMD=%%i"
)
if not defined MAVEN_CMD (
  echo [ERROR] Maven not found. Please install Maven or edit start-dev.bat.
  pause
  exit /b 1
)

echo.
echo [1/2] Starting backend Spring Boot :8080
start "Backend - Spring Boot :8080" cmd /k "%MAVEN_CMD% spring-boot:run"

echo [2/2] Starting frontend Vite :5173
start "Frontend - Vite :5173" cmd /k "cd /d web && pnpm dev"

echo.
echo All started. Open in your browser:
echo   Portal    http://localhost:5173/
echo   User App  http://localhost:5173/chat
echo   Admin     http://localhost:5173/admin
echo.
pause
