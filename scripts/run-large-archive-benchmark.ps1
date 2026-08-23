param(
    [int[]]$EntryCounts = @(10000, 50000, 200000),
    [int]$Port = 18767,
    [string]$JdkHome = 'C:\Program Files\Java\jdk-17',
    [string]$ReportPath = (Join-Path (Split-Path -Parent $PSScriptRoot) 'docs\2026-08-23-large-archive-benchmark.md')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($PSVersionTable.PSVersion.Major -lt 7) {
    throw '大包性能基准必须使用 PowerShell 7 或更高版本。'
}

$ProjectRoot = [System.IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$BackendJar = Join-Path $ProjectRoot 'backend\target\jarpatch-studio-backend.jar'
$Java = Join-Path $JdkHome 'bin\java.exe'
$Javac = Join-Path $JdkHome 'bin\javac.exe'
$TokenHeader = 'X-JarPatch-Token'
$AuthToken = [System.Convert]::ToHexString([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32)).ToLowerInvariant()
$InstanceId = [guid]::NewGuid().ToString()
$BaseUrl = "http://127.0.0.1:$Port"
$TempRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$WorkRoot = Join-Path $TempRoot ("jarpatch-benchmark-" + [guid]::NewGuid())
$BackendHome = Join-Path $WorkRoot 'backend-home'
$SampleRoot = Join-Path $WorkRoot 'samples'
$BackendStdout = Join-Path $WorkRoot 'backend.stdout.log'
$BackendStderr = Join-Path $WorkRoot 'backend.stderr.log'
$ReportEncoding = [System.Text.UTF8Encoding]::new($false)
$HttpClient = [System.Net.Http.HttpClient]::new()
$HttpClient.Timeout = [TimeSpan]::FromHours(1)
$BackendProcess = $null
$Results = [System.Collections.Generic.List[object]]::new()

function Assert-RequiredFile {
    param([Parameter(Mandatory)][string]$Path, [Parameter(Mandatory)][string]$Description)
    if (!(Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "未找到${Description}：$Path"
    }
}

function New-BenchmarkClass {
    $SourceDir = Join-Path $WorkRoot 'source\sample'
    $ClassDir = Join-Path $WorkRoot 'classes'
    [System.IO.Directory]::CreateDirectory($SourceDir) | Out-Null
    [System.IO.Directory]::CreateDirectory($ClassDir) | Out-Null
    $SourcePath = Join-Path $SourceDir 'BenchmarkMarker.java'
    $Source = @'
package sample;

public class BenchmarkMarker {
    public static int value() {
        return 1;
    }
}
'@
    [System.IO.File]::WriteAllText($SourcePath, $Source, $ReportEncoding)
    & $Javac '--release' '8' '-encoding' 'UTF-8' '-d' $ClassDir $SourcePath
    if ($LASTEXITCODE -ne 0) {
        throw "基准 class 编译失败，退出码：$LASTEXITCODE"
    }
    return Join-Path $ClassDir 'sample\BenchmarkMarker.class'
}

function New-LargeArchive {
    param(
        [Parameter(Mandatory)][int]$EntryCount,
        [Parameter(Mandatory)][string]$ClassFile
    )
    if ($EntryCount -lt 3 -or $EntryCount -gt 200000) {
        throw "条目数必须在 3 到 200000 之间：$EntryCount"
    }
    [System.IO.Directory]::CreateDirectory($SampleRoot) | Out-Null
    $ArchivePath = Join-Path $SampleRoot ("benchmark-$EntryCount.jar")
    $FileStream = [System.IO.File]::Open($ArchivePath, [System.IO.FileMode]::Create,
        [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
    try {
        $Archive = [System.IO.Compression.ZipArchive]::new(
            $FileStream, [System.IO.Compression.ZipArchiveMode]::Create, $false)
        try {
            $ManifestEntry = $Archive.CreateEntry('META-INF/MANIFEST.MF',
                [System.IO.Compression.CompressionLevel]::Fastest)
            $ManifestWriter = [System.IO.StreamWriter]::new($ManifestEntry.Open(), $ReportEncoding)
            try {
                $ManifestWriter.Write("Manifest-Version: 1.0`r`n`r`n")
            }
            finally {
                $ManifestWriter.Dispose()
            }

            $ClassEntry = $Archive.CreateEntry('sample/BenchmarkMarker.class',
                [System.IO.Compression.CompressionLevel]::Fastest)
            $ClassInput = [System.IO.File]::OpenRead($ClassFile)
            $ClassOutput = $ClassEntry.Open()
            try {
                $ClassInput.CopyTo($ClassOutput)
            }
            finally {
                $ClassOutput.Dispose()
                $ClassInput.Dispose()
            }

            $ResourceCount = $EntryCount - 2
            for ($Index = 0; $Index -lt $ResourceCount; $Index++) {
                $Group = [int][math]::Floor($Index / 1000)
                $EntryName = 'payload/group{0:D4}/file{1:D6}.txt' -f $Group, $Index
                $Entry = $Archive.CreateEntry($EntryName, [System.IO.Compression.CompressionLevel]::Fastest)
                $Writer = [System.IO.StreamWriter]::new($Entry.Open(), $ReportEncoding)
                try {
                    if ($Index -eq ($ResourceCount - 1)) {
                        $Writer.Write("benchmark-hit-$EntryCount")
                    }
                    else {
                        $Writer.Write("entry-$Index")
                    }
                }
                finally {
                    $Writer.Dispose()
                }
            }
        }
        finally {
            $Archive.Dispose()
        }
    }
    finally {
        $FileStream.Dispose()
    }
    return $ArchivePath
}

function Invoke-TimedApi {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string]$Method,
        [Parameter(Mandatory)][string]$Path,
        [object]$Body
    )
    $Request = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::new($Method), "$BaseUrl$Path")
    $Request.Headers.Add($TokenHeader, $AuthToken)
    if ($null -ne $Body) {
        $Json = $Body | ConvertTo-Json -Depth 20 -Compress
        $Request.Content = [System.Net.Http.StringContent]::new(
            $Json, [System.Text.Encoding]::UTF8, 'application/json')
    }
    $Stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $PeakWorkingSet = 0L
    try {
        $Task = $HttpClient.SendAsync($Request)
        while (!$Task.IsCompleted) {
            if ($null -ne $BackendProcess -and !$BackendProcess.HasExited) {
                $BackendProcess.Refresh()
                $PeakWorkingSet = [math]::Max($PeakWorkingSet, $BackendProcess.WorkingSet64)
            }
            Start-Sleep -Milliseconds 50
        }
        $Response = $Task.GetAwaiter().GetResult()
        $Content = $Response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (!$Response.IsSuccessStatusCode) {
            throw "$Name 请求失败：HTTP $([int]$Response.StatusCode)，$Content"
        }
        $Payload = $Content | ConvertFrom-Json -Depth 30
        if (!$Payload.success) {
            throw "$Name 业务失败：$($Payload.message)"
        }
        return [pscustomobject]@{
            Name = $Name
            ElapsedMs = $Stopwatch.ElapsedMilliseconds
            PeakMemoryMiB = [math]::Round($PeakWorkingSet / 1MB, 2)
            Data = $Payload.data
        }
    }
    finally {
        $Stopwatch.Stop()
        $Request.Dispose()
    }
}

function Wait-BackendReady {
    $Deadline = [DateTimeOffset]::UtcNow.AddSeconds(30)
    while ([DateTimeOffset]::UtcNow -lt $Deadline) {
        if ($BackendProcess.HasExited) {
            throw "基准后端在就绪前退出：$([System.IO.File]::ReadAllText($BackendStderr))"
        }
        try {
            $Health = Invoke-TimedApi -Name '健康检查' -Method 'GET' -Path '/api/system/health'
            if ($Health.Data.status -eq 'UP' -and $Health.Data.instanceId -eq $InstanceId) {
                return
            }
        }
        catch {
            Start-Sleep -Milliseconds 250
        }
    }
    throw '等待基准后端健康检查超时。'
}

function Invoke-ArchiveTier {
    param([Parameter(Mandatory)][int]$EntryCount, [Parameter(Mandatory)][string]$ClassFile)

    Write-Host "生成 $EntryCount 条目基准包..."
    $ArchivePath = New-LargeArchive -EntryCount $EntryCount -ClassFile $ClassFile
    $Import = Invoke-TimedApi -Name '导入' -Method 'POST' -Path '/api/projects/import' -Body @{
        filePath = $ArchivePath
        selectedNestedJars = @()
        taskId = $null
    }
    $Project = $Import.Data
    $ProjectId = $Project.id

    $Tree = Invoke-TimedApi -Name '初始文件树' -Method 'GET' -Path "/api/projects/$ProjectId/tree"
    $TreeChildren = Invoke-TimedApi -Name '目录懒加载' -Method 'GET' `
        -Path "/api/projects/$ProjectId/tree/children?path=extracted"
    $SearchKeyword = "benchmark-hit-$EntryCount"
    $Search = Invoke-TimedApi -Name '全量搜索' -Method 'GET' `
        -Path "/api/projects/$ProjectId/search?keyword=$([uri]::EscapeDataString($SearchKeyword))"

    $SourcePath = 'sources/sample/BenchmarkMarker.java'
    $Read = Invoke-TimedApi -Name '读取源码' -Method 'GET' `
        -Path "/api/projects/$ProjectId/files/content?path=$([uri]::EscapeDataString($SourcePath))"
    $UpdatedSource = $Read.Data.content -replace 'return 1;', 'return 2;'
    if ($UpdatedSource -eq $Read.Data.content) {
        throw "CFR 输出中未找到预期 return 1;：$EntryCount"
    }
    $Save = Invoke-TimedApi -Name '保存源码' -Method 'PUT' -Path "/api/projects/$ProjectId/files/content" -Body @{
        path = $SourcePath
        content = $UpdatedSource
        expectedHash = $Read.Data.contentHash
        encoding = $Read.Data.encoding
    }
    $Compile = Invoke-TimedApi -Name '编译' -Method 'POST' -Path "/api/projects/$ProjectId/compile"
    $OutputPath = Join-Path $SampleRoot ("benchmark-$EntryCount-patched.jar")
    $Export = Invoke-TimedApi -Name '导出' -Method 'POST' -Path "/api/projects/$ProjectId/export" -Body @{
        outputPath = $OutputPath
        signaturePolicy = 'REMOVE_INVALID_SIGNATURES'
        taskId = $null
    }

    $Results.Add([pscustomobject]@{
        EntryCount = $EntryCount
        ArchiveMiB = [math]::Round((Get-Item -LiteralPath $ArchivePath).Length / 1MB, 2)
        ImportMs = $Import.ElapsedMs
        TreeMs = $Tree.ElapsedMs
        LazyChildrenMs = $TreeChildren.ElapsedMs
        SearchMs = $Search.ElapsedMs
        CompileMs = $Compile.ElapsedMs
        ExportMs = $Export.ElapsedMs
        PeakMemoryMiB = @($Import, $Tree, $TreeChildren, $Search, $Compile, $Export |
                Measure-Object -Property PeakMemoryMiB -Maximum).Maximum
        SearchResults = @($Search.Data).Count
        ExportMiB = [math]::Round((Get-Item -LiteralPath $OutputPath).Length / 1MB, 2)
    })

    $Preview = Invoke-TimedApi -Name '工作区清理预览' -Method 'GET' `
        -Path "/api/projects/$ProjectId/workspace/cleanup-preview"
    Invoke-TimedApi -Name '工作区清理' -Method 'DELETE' `
        -Path "/api/projects/$ProjectId/workspace?confirmationId=$($Preview.Data.confirmationId)" | Out-Null
    Invoke-TimedApi -Name '历史清理' -Method 'DELETE' -Path "/api/projects/$ProjectId" | Out-Null
}

function Write-BenchmarkReport {
    $GitCommit = (& git.exe -C $ProjectRoot rev-parse HEAD).ToString().Trim()
    $Rows = $Results | ForEach-Object {
        "| $($_.EntryCount) | $($_.ArchiveMiB) | $($_.ImportMs) | $($_.TreeMs) | $($_.LazyChildrenMs) | $($_.SearchMs) | $($_.CompileMs) | $($_.ExportMs) | $($_.PeakMemoryMiB) | $($_.ExportMiB) |"
    }
    $Report = @"
# JarPatch Studio 大包性能基准

## 基准范围

- 执行时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')
- Git commit：$GitCommit
- PowerShell：$($PSVersionTable.PSVersion)
- Java：$(& $Java -version 2>&1 | Select-Object -First 1)
- 样本：标准 JAR，每档包含 1 个 Java 8 class、1 个 Manifest，其余为文本资源。
- 流程：导入并批量反编译、初始懒加载树、一级目录加载、全量内容搜索、修改 1 个源码、编译、流式导出。
- 内存：操作期间后端 JVM 的峰值 Working Set；同一 JVM 按 1 万、5 万、20 万条目依次执行并保留预热效果。

## 结果

| 条目数 | 原包 MiB | 导入 ms | 初始树 ms | 一级懒加载 ms | 搜索 ms | 编译 ms | 导出 ms | JVM 峰值 MiB | 导出包 MiB |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
$($Rows -join "`n")

## 结论口径

本报告记录的是当前机器实测值，不外推其他硬件。初始文件树只返回固定根节点，目录展开只读取直接子项；导出按目录稳定排序并直接写 Zip，不预先收集全部路径；反编译通过 CFR 主 API 分批执行。20 万条目是当前默认条目上限，超过上限会被资源门禁明确拒绝。
"@
    [System.IO.File]::WriteAllText([System.IO.Path]::GetFullPath($ReportPath), $Report, $ReportEncoding)
}

Assert-RequiredFile -Path $BackendJar -Description '完整后端 Jar'
Assert-RequiredFile -Path $Java -Description 'JDK 17 java.exe'
Assert-RequiredFile -Path $Javac -Description 'JDK 17 javac.exe'
[System.IO.Directory]::CreateDirectory($WorkRoot) | Out-Null
[System.IO.Directory]::CreateDirectory($BackendHome) | Out-Null

$PreviousToken = $env:JARPATCH_AUTH_TOKEN
$PreviousInstance = $env:JARPATCH_INSTANCE_ID
$PreviousLog = $env:JARPATCH_LOG_FILE
try {
    $env:JARPATCH_AUTH_TOKEN = $AuthToken
    $env:JARPATCH_INSTANCE_ID = $InstanceId
    $env:JARPATCH_LOG_FILE = Join-Path $BackendHome 'logs\backend.log'
    $BackendProcess = Start-Process -FilePath $Java -ArgumentList @(
        "-Duser.home=$BackendHome", '-jar', $BackendJar, "--server.port=$Port") -PassThru -WindowStyle Hidden `
        -RedirectStandardOutput $BackendStdout -RedirectStandardError $BackendStderr
    Wait-BackendReady
    $ClassFile = New-BenchmarkClass
    foreach ($EntryCount in $EntryCounts) {
        Invoke-ArchiveTier -EntryCount $EntryCount -ClassFile $ClassFile
    }
    Write-BenchmarkReport
    Write-Host "性能基准完成：$ReportPath"
}
finally {
    if ($null -ne $BackendProcess -and !$BackendProcess.HasExited) {
        try {
            Invoke-TimedApi -Name '关闭后端' -Method 'POST' -Path '/api/system/shutdown' | Out-Null
            $ExitedGracefully = $BackendProcess.WaitForExit(10000)
            if (!$ExitedGracefully) {
                $BackendProcess.Kill($true)
                $BackendProcess.WaitForExit(10000) | Out-Null
            }
        }
        catch {
            if (!$BackendProcess.HasExited) {
                $BackendProcess.Kill($true)
                $BackendProcess.WaitForExit(10000) | Out-Null
            }
        }
    }
    $HttpClient.Dispose()
    $env:JARPATCH_AUTH_TOKEN = $PreviousToken
    $env:JARPATCH_INSTANCE_ID = $PreviousInstance
    $env:JARPATCH_LOG_FILE = $PreviousLog
    $NormalizedWorkRoot = [System.IO.Path]::GetFullPath($WorkRoot)
    if (($NormalizedWorkRoot.StartsWith($TempRoot, [System.StringComparison]::OrdinalIgnoreCase)) -and
            ($NormalizedWorkRoot -ne $TempRoot) -and
            ([System.IO.Directory]::Exists($NormalizedWorkRoot))) {
        [System.IO.Directory]::Delete($NormalizedWorkRoot, $true)
    }
}
