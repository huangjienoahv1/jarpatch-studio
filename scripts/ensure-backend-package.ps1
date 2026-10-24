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
$BuildStampPath = "$FullJarPath.source.json"

<#
.SYNOPSIS
按稳定相对路径汇总 Maven 构建文件、后端源码和资源内容摘要，识别新增、修改和删除文件。
.OUTPUTS
System.String。所有构建输入组成的 SHA-256。
#>
function Get-BackendSourceFingerprint {
    $SourceRoot = Join-Path $FullProjectRoot 'backend/src'
    $Inputs = @(
        (Join-Path $FullProjectRoot 'pom.xml'),
        (Join-Path $FullProjectRoot 'backend/pom.xml')
    ) + @(Get-ChildItem -LiteralPath $SourceRoot -Recurse -File | Select-Object -ExpandProperty FullName)
    $SourceRows = foreach ($InputPath in ($Inputs | Sort-Object)) {
        $RelativePath = [System.IO.Path]::GetRelativePath($FullProjectRoot, $InputPath).Replace('\', '/')
        "$RelativePath`0$((Get-FileHash -LiteralPath $InputPath -Algorithm SHA256).Hash)"
    }
    $Digest = [System.Security.Cryptography.SHA256]::Create()
    try {
        $Bytes = [System.Text.Encoding]::UTF8.GetBytes(($SourceRows -join "`n"))
        return [System.BitConverter]::ToString($Digest.ComputeHash($Bytes)).Replace('-', '').ToLowerInvariant()
    }
    finally {
        $Digest.Dispose()
    }
}

<#
.SYNOPSIS
记录已经完成且构建期间输入未变化的后端包与源码摘要；通过同目录临时文件更新记录。
#>
function Write-BackendBuildStamp {
    param([Parameter(Mandatory)][string]$SourceFingerprint)
    $Stamp = [ordered]@{
        sourceSha256 = $SourceFingerprint
        jarSha256 = (Get-FileHash -LiteralPath $FullJarPath -Algorithm SHA256).Hash.ToLowerInvariant()
    }
    $TemporaryStamp = "$BuildStampPath.$([Guid]::NewGuid().ToString('N')).tmp"
    try {
        [System.IO.File]::WriteAllText($TemporaryStamp, ($Stamp | ConvertTo-Json),
            [System.Text.UTF8Encoding]::new($false))
        [System.IO.File]::Move($TemporaryStamp, $BuildStampPath, $true)
    }
    finally {
        if (Test-Path -LiteralPath $TemporaryStamp) {
            Remove-Item -LiteralPath $TemporaryStamp
        }
    }
}

<#
.SYNOPSIS
检查后端 JAR 结构及构建记录，确认包内容摘要和当前源码摘要一致。

.OUTPUTS
System.Boolean。结构完整、源码及包内容均与构建记录一致时返回 true。
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
        }
        finally {
            $Archive.Dispose()
        }
        if (!(Test-Path -LiteralPath $BuildStampPath -PathType Leaf)) {
            Write-Warning '后端包没有源码构建记录，需要重新构建一次。'
            return $false
        }
        $Stamp = Get-Content -LiteralPath $BuildStampPath -Raw | ConvertFrom-Json
        if ($Stamp.sourceSha256 -ne (Get-BackendSourceFingerprint) -or
            $Stamp.jarSha256 -ne (Get-FileHash -LiteralPath $FullJarPath -Algorithm SHA256).Hash.ToLowerInvariant()) {
            Write-Warning '后端源码或包内容与构建记录不一致，需要重新构建。'
            return $false
        }
        return $true
    }
    catch {
        Write-Warning "后端包或构建记录校验失败：$($_.Exception.Message)"
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

    $SourceFingerprint = Get-BackendSourceFingerprint
    Stop-ExistingBackendProcesses
    Push-Location $FullProjectRoot
    try {
        & $Maven.Source '-Dmaven.test.skip=true' package
        if ($LASTEXITCODE -ne 0) {
            throw "后端重新构建失败，Maven 退出码：$LASTEXITCODE"
        }
        if ($SourceFingerprint -ne (Get-BackendSourceFingerprint)) {
            throw '后端构建期间源码发生变化，请重新启动构建。'
        }
        Write-BackendBuildStamp -SourceFingerprint $SourceFingerprint
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
            throw '后端重新构建完成，但包结构或源码构建记录复验未通过。'
        }
    }
}

Write-Host "后端包完整性检查通过：$FullJarPath"
