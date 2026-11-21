@echo off
setlocal EnableExtensions DisableDelayedExpansion

set "WIN7_BUILD_JDK=%~1"
if not defined WIN7_BUILD_JDK set "WIN7_BUILD_JDK=%JARPATCH_WIN7_JDK_HOME%"
if not defined WIN7_BUILD_JDK set "WIN7_BUILD_JDK=%TEMP%\jarpatch-win7-dependencies\jdk-21"

set "WIN7_NATIVE_IMAGE_HOME=%~2"
if not defined WIN7_NATIVE_IMAGE_HOME set "WIN7_NATIVE_IMAGE_HOME=%NIK_HOME%"
if not defined WIN7_NATIVE_IMAGE_HOME set "WIN7_NATIVE_IMAGE_HOME=%GRAALVM_HOME%"
if not defined WIN7_NATIVE_IMAGE_HOME set "WIN7_NATIVE_IMAGE_HOME=%TEMP%\jarpatch-win7-dependencies\nik-17"

set "WIN7_BUILD_EXIT_CODE=1"
where pwsh.exe >nul 2>nul
if errorlevel 1 (
    echo PowerShell 7 was not found on the build computer.
    goto finish
)

if not exist "%WIN7_BUILD_JDK%\bin\javac.exe" (
    echo BellSoft Liberica JDK 21 x64 was not found at "%WIN7_BUILD_JDK%".
    goto usage
)

if not exist "%WIN7_NATIVE_IMAGE_HOME%\bin\native-image.cmd" if not exist "%WIN7_NATIVE_IMAGE_HOME%\bin\native-image.exe" (
    echo BellSoft Liberica Native Image Kit 23.0 was not found at "%WIN7_NATIVE_IMAGE_HOME%".
    goto usage
)

pwsh.exe -NoLogo -NoProfile -File "%~dp0build.ps1" -Windows7 -NativeBackend -JdkHome "%WIN7_BUILD_JDK%\." -NativeImageHome "%WIN7_NATIVE_IMAGE_HOME%\."
set "WIN7_BUILD_EXIT_CODE=%ERRORLEVEL%"
goto finish

:usage
echo Usage: %~nx0 "C:\Tools\LibericaJDK21" "C:\Tools\LibericaNIK17"

:finish
if "%~1"=="" pause
exit /b %WIN7_BUILD_EXIT_CODE%
