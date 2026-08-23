param(
    [Parameter(Mandatory)]
    [string]$JarPath,
    [Parameter(Mandatory)]
    [string]$ProjectRoot,
    [switch]$ValidateOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($PSVersionTable.PSVersion.Major -lt 7) {
    throw '后端包完整性检查必须使用 PowerShell 7 或更高版本。'
}

$RequiredEntries = @(
    'BOOT-INF/classes/com/jarpatch/JarPatchStudioApplication.class',
    'BOOT-INF/lib/cfr-0.152.jar'
)
$JavaProcessNames = @('java.exe', 'javaw.exe')
$FullJarPath = [System.IO.Path]::GetFullPath($JarPath)
$FullProjectRoot = [System.IO.Path]::GetFullPath($ProjectRoot)

<#
.SYNOPSIS
检查后端 JAR 是否是包含应用入口和 CFR 依赖的 Spring Boot 可执行包。

.OUTPUTS
System.Boolean。所有必需条目存在时返回 true；文件缺失、损坏或条目不完整时返回 false。
#>
function Test-BackendPackage {
    if (!(Test-Path -LiteralPath $FullJarPath -PathType Leaf)) {
        Write-Warning "后端包不存在：$FullJarPath"
        return $false
    }

    try {
        $Archive = [System.IO.Compression.ZipFile]::OpenRead($FullJarPath)
        try {
            $EntryNames = [System.Collections.Generic.HashSet[string]]::new(
                [System.StringComparer]::Ordinal
            )
            foreach ($Entry in $Archive.Entries) {
                [void]$EntryNames.Add($Entry.FullName)
            }
            $MissingEntries = @($RequiredEntries | Where-Object { !$EntryNames.Contains($_) })
            if ($MissingEntries.Count -gt 0) {
                Write-Warning "后端包缺少必需条目：$($MissingEntries -join ', ')"
                return $false
            }
            return $true
        }
        finally {
            $Archive.Dispose()
        }
    }
    catch {
        Write-Warning "后端包无法作为 ZIP/JAR 读取：$($_.Exception.Message)"
        return $false
    }
}

<#
.SYNOPSIS
停止命令行中精确引用当前后端包路径的旧 Java 进程。

.DESCRIPTION
只处理 java.exe/javaw.exe 且命令行包含规范化后端 JAR 绝对路径的进程，避免扩大停止范围。
#>
function Stop-ExistingBackendProcesses {
    $Processes = @(Get-CimInstance Win32_Process | Where-Object {
        $_.Name -in $JavaProcessNames -and
        ![string]::IsNullOrWhiteSpace($_.CommandLine) -and
        $_.CommandLine.Contains($FullJarPath, [System.StringComparison]::OrdinalIgnoreCase)
    })
    foreach ($Process in $Processes) {
        Write-Host "正在停止占用后端包的旧进程：PID=$($Process.ProcessId)"
        Stop-Process -Id $Process.ProcessId -Force
        Wait-Process -Id $Process.ProcessId -Timeout 10 -ErrorAction SilentlyContinue
    }
}

<#
.SYNOPSIS
使用项目 Maven 入口重新生成后端 Spring Boot 可执行包。

.DESCRIPTION
入口是稳定启动脚本；实际构建在项目根目录执行，结果写入 backend/target 后再次进行结构校验。
#>
function Build-BackendPackage {
    $Maven = Get-Command 'mvn.cmd' -ErrorAction SilentlyContinue
    if ($null -eq $Maven) {
        throw '后端包不完整，且未找到 Maven 命令 mvn.cmd，无法重新构建。'
    }

    Stop-ExistingBackendProcesses
    Push-Location $FullProjectRoot
    try {
        & $Maven.Source '-Dmaven.test.skip=true' package
        if ($LASTEXITCODE -ne 0) {
            throw "后端重新构建失败，Maven 退出码：$LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

$BackendPackageValid = Test-BackendPackage
if (!$BackendPackageValid) {
    if ($ValidateOnly) {
        throw '后端包完整性检查未通过。'
    }
    else {
        Build-BackendPackage
        if (!(Test-BackendPackage)) {
            throw '后端重新构建完成，但生成的 JAR 仍缺少应用入口或 CFR 依赖。'
        }
    }
}

Write-Host "后端包完整性检查通过：$FullJarPath"
