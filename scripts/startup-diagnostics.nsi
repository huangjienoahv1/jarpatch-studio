Unicode true
RequestExecutionLevel user
SilentInstall silent
ManifestSupportedOS all
Name "JarPatch Studio 启动诊断"
OutFile "${OUTPUT_FILE}"
SetCompressor /SOLID lzma
!include "LogicLib.nsh"
!include "FileFunc.nsh"
!include "WinVer.nsh"
!include "x64.nsh"

Var LogDirectory
Var LogFile
Var LogHandle
Var DesktopLog
Var ChromiumLog
Var AppPath
Var ExitCode
Var ExitHex
Var Parameters
Var NoOpen
Var AppArguments

!macro WriteLog TEXT
  FileWriteUTF16LE $LogHandle "${TEXT}$\r$\n"
!macroend

Section
  ${GetParameters} $Parameters
  ClearErrors
  ${GetOptions} $Parameters "/NOOPEN" $0
  StrCpy $NoOpen "0"
  IfErrors +2
    StrCpy $NoOpen "1"
  StrCpy $AppArguments ""
  ClearErrors
  ${GetOptions} $Parameters "/SMOKE" $0
  IfErrors +2
    StrCpy $AppArguments " --smoke-check"
  ${GetTime} "" "L" $0 $1 $2 $3 $4 $5 $6
  StrCpy $LogDirectory "$LOCALAPPDATA\JarPatch Studio\startup-diagnostics\$2$1$0-$4$5$6"
  System::Call 'kernel32::GetCurrentProcessId() i .r0'
  StrCpy $LogDirectory "$LogDirectory-$0"
  CreateDirectory "$LogDirectory"
  StrCpy $LogFile "$LogDirectory\launcher.log"
  StrCpy $DesktopLog "$LogDirectory\desktop-startup.log"
  StrCpy $ChromiumLog "$LogDirectory\chromium.log"
  ClearErrors
  FileOpen $LogHandle "$LogFile" w
  IfErrors log_unavailable
  FileWriteUTF16LE /BOM $LogHandle "JarPatch Studio 启动诊断$\r$\n"
  StrCpy $AppPath "$EXEDIR\${APP_EXE}"
  !insertmacro WriteLog "应用文件：$AppPath"
  !insertmacro WriteLog "桌面启动日志：$DesktopLog"
  !insertmacro WriteLog "Chromium 日志：$ChromiumLog"
  StrCpy $ExitCode 0
  ${IfNot} ${RunningX64}
    !insertmacro WriteLog "不兼容：目标程序需要 64 位 Windows。"
    StrCpy $ExitCode 3
    Goto finish
  ${EndIf}
!ifdef WINDOWS7
  ${IfNot} ${AtLeastWin7}
    !insertmacro WriteLog "不兼容：需要 Windows 7 SP1 或更高版本。"
    StrCpy $ExitCode 3
    Goto finish
  ${EndIf}
  ${If} ${IsWin7}
  ${AndIfNot} ${AtLeastServicePack} 1
    !insertmacro WriteLog "不兼容：当前 Windows 7 未安装 Service Pack 1。"
    StrCpy $ExitCode 3
    Goto finish
  ${EndIf}
!else
  ${IfNot} ${AtLeastWin10}
    !insertmacro WriteLog "不兼容：当前是标准 Windows 包，需要 Windows 10 或更高版本；Win7 请使用文件名带 Windows7 的包。"
    StrCpy $ExitCode 3
    Goto finish
  ${EndIf}
!endif
  IfFileExists "$AppPath" +4
    !insertmacro WriteLog "无法启动：请把本诊断器与上述 EXE 放在同一目录。"
    StrCpy $ExitCode 2
    Goto finish
  System::Call 'kernel32::SetEnvironmentVariableW(w "JARPATCH_STARTUP_LOG", w "$DesktopLog") i .r0'
  System::Call 'kernel32::SetEnvironmentVariableW(w "ELECTRON_LOG_FILE", w "$ChromiumLog") i .r0'
  System::Call 'kernel32::SetEnvironmentVariableW(w "ELECTRON_RUN_AS_NODE", p 0) i .r0'
  !insertmacro WriteLog "正在启动；若程序打开，请关闭主程序后查看诊断结果。"
  ClearErrors
  ExecWait '"$AppPath" --enable-logging=file --log-file="$ChromiumLog"$AppArguments' $ExitCode
  IfErrors launch_failed
  IntFmt $ExitHex "0x%08X" $ExitCode
  !insertmacro WriteLog "应用退出码：$ExitCode ($ExitHex)"
  ${Select} $ExitHex
    ${Case} "0xC0000135"
      !insertmacro WriteLog "系统加载错误：缺少所需 DLL（STATUS_DLL_NOT_FOUND）。"
    ${Case} "0xC0000139"
      !insertmacro WriteLog "系统加载错误：找不到 DLL 入口点，可能是系统版本或 DLL 版本不匹配。"
    ${Case} "0xC000007B"
      !insertmacro WriteLog "系统加载错误：EXE/DLL 格式或位数不匹配。"
    ${Case} "0xC0000142"
      !insertmacro WriteLog "系统加载错误：DLL 初始化失败。"
    ${Case} "0xC0000005"
      !insertmacro WriteLog "程序异常：访问冲突。"
    ${Case} "0xC000001D"
      !insertmacro WriteLog "程序异常：处理器不支持所执行的指令。"
    ${Case} "0x00000000"
      !insertmacro WriteLog "启动器报告正常退出；这不等于界面已成功打开，请结合桌面启动日志中的 READY / STARTUP_FAILED 判断。"
    ${CaseElse}
      !insertmacro WriteLog "程序退出异常，请查看桌面启动日志、Chromium 日志和 Windows 应用程序事件。"
  ${EndSelect}
  IfFileExists "$DesktopLog" finish
    !insertmacro WriteLog "未生成桌面启动日志：可能在 Electron 加载之前失败、被拦截，或当前应用版本尚不包含桌面启动日志功能。"
  Goto finish
launch_failed:
  System::Call 'kernel32::GetLastError() i .r0'
  !insertmacro WriteLog "无法创建应用进程；系统错误码（尽力读取）：$0。请确认文件存在、未被拦截且可在当前系统执行。"
  StrCpy $ExitCode 2
finish:
  !insertmacro WriteLog "日志目录：$LogDirectory"
  FileClose $LogHandle
  ${If} $NoOpen != "1"
    ExecShell "open" "$LogFile"
    ExecShell "open" "$LogDirectory"
  ${EndIf}
  SetErrorLevel 0
  ${If} $ExitCode != 0
    SetErrorLevel 1
  ${EndIf}
  Quit
log_unavailable:
  MessageBox MB_OK|MB_ICONSTOP "无法创建诊断日志：$LogFile。请检查当前用户的本地应用数据目录权限。"
  SetErrorLevel 2
SectionEnd
