param(
    [int]$Port = 18766,
    [string]$Jdk17Home = 'C:\Program Files\Java\jdk-17'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($PSVersionTable.PSVersion.Major -lt 7) {
    throw '真实样本验收必须使用 PowerShell 7 或更高版本。'
}

$SampleRoot = [System.IO.Path]::GetFullPath($PSScriptRoot)
$ProjectRoot = [System.IO.Path]::GetFullPath((Join-Path $SampleRoot '..\..'))
$OutputRoot = Join-Path $SampleRoot 'output'
$RuntimeHome = Join-Path $SampleRoot 'work\runtime-home'
$BackendJava = Join-Path $ProjectRoot 'frontend\runtime\bin\java.exe'
$BackendJar = Join-Path $ProjectRoot 'backend\target\jarpatch-studio-backend.jar'
$BaseUrl = "http://127.0.0.1:$Port"
$Token = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
$InstanceId = [Guid]::NewGuid().ToString()
$Headers = @{ 'X-JarPatch-Token' = $Token }
$BackendProcess = $null

function Invoke-Api {
    param(
        [Parameter(Mandatory)]
        [string]$Method,
        [Parameter(Mandatory)]
        [string]$Path,
        [object]$Body
    )

    $Parameters = @{
        Uri = "$BaseUrl$Path"
        Method = $Method
        Headers = $Headers
        TimeoutSec = 180
    }
    if ($null -ne $Body) {
        $Parameters.ContentType = 'application/json'
        $Parameters.Body = $Body | ConvertTo-Json -Depth 12 -Compress
    }
    $Response = Invoke-RestMethod @Parameters
    if (!$Response.success) {
        throw "接口失败：$Method $Path，$($Response.message)"
    }
    return $Response.data
}

function Import-Sample {
    param(
        [Parameter(Mandatory)]
        [string]$FileName
    )

    $FilePath = Join-Path $OutputRoot $FileName
    if (!(Test-Path -LiteralPath $FilePath)) {
        throw "验收样本不存在：$FilePath"
    }
    return Invoke-Api 'POST' '/api/projects/import' @{
        filePath = $FilePath
        selectedNestedJars = @()
    }
}

function Invoke-EditCompileExport {
    param(
        [Parameter(Mandatory)]
        [object]$Project,
        [Parameter(Mandatory)]
        [string]$SourceName,
        [Parameter(Mandatory)]
        [string]$OriginalText,
        [Parameter(Mandatory)]
        [string]$UpdatedText
    )

    $SourceRoot = Join-Path $Project.workspacePath 'sources'
    $SourceFiles = @(Get-ChildItem -LiteralPath $SourceRoot -Recurse -File -Filter $SourceName)
    if ($SourceFiles.Count -ne 1) {
        throw "验收源码数量不正确：$SourceName，实际 $($SourceFiles.Count)"
    }
    $RelativeSourcePath = 'sources/' + [System.IO.Path]::GetRelativePath($SourceRoot, $SourceFiles[0].FullName).Replace('\', '/')
    $EncodedPath = [Uri]::EscapeDataString($RelativeSourcePath)
    $ContentView = Invoke-Api 'GET' "/api/projects/$($Project.id)/files/content?path=$EncodedPath"
    $UpdatedContent = $ContentView.content.Replace($OriginalText, $UpdatedText)
    if ($UpdatedContent -eq $ContentView.content) {
        throw "验收源码未包含预期文本：$OriginalText"
    }
    $SavedView = Invoke-Api 'PUT' "/api/projects/$($Project.id)/files/content" @{
        path = $RelativeSourcePath
        content = $UpdatedContent
        expectedHash = $ContentView.contentHash
    }
    if (!$SavedView.changed) {
        throw "验收源码修改未被记录：$RelativeSourcePath"
    }

    $CompileResult = Invoke-Api 'POST' "/api/projects/$($Project.id)/compile"
    $Diff = Invoke-Api 'GET' "/api/projects/$($Project.id)/diff"
    if ($Diff.sourceDiffs.Count -lt 1 -or $Diff.compiledArtifacts.Count -lt 1) {
        throw "差异或编译产物未形成闭环：$($Project.name)"
    }

    $ExportRoot = Join-Path $OutputRoot 'exports'
    New-Item -ItemType Directory -Path $ExportRoot -Force | Out-Null
    $OutputPath = Join-Path $ExportRoot ("$($Project.id)-$($Project.name)")
    $ExportResult = Invoke-Api 'POST' "/api/projects/$($Project.id)/export" @{
        outputPath = $OutputPath
        signaturePolicy = 'PRESERVE_ONLY_UNMODIFIED'
    }
    if (!$ExportResult.validation.valid -or !(Test-Path -LiteralPath $OutputPath)) {
        throw "导出结构校验未通过：$($Project.name)"
    }
    $TaskLogs = @(Invoke-Api 'GET' "/api/tasks/$($CompileResult.taskId)/logs")
    if ($TaskLogs.Count -lt 2) {
        throw "编译任务持久化日志不完整：$($Project.name)"
    }

    return [ordered]@{
        projectId = $Project.id
        targetJavaVersion = $Project.targetJavaVersion
        packageType = $Project.packageType
        sourcePath = $RelativeSourcePath
        compiledArtifactCount = $Diff.compiledArtifacts.Count
        taskLogCount = $TaskLogs.Count
        outputPath = $ExportResult.outputPath
        exportValid = $ExportResult.validation.valid
    }
}

function Assert-Risk {
    param(
        [Parameter(Mandatory)]
        [object]$Project,
        [Parameter(Mandatory)]
        [string]$ExpectedTitle
    )

    $Report = Invoke-Api 'POST' "/api/projects/$($Project.id)/analyze"
    if (@($Report.risks | Where-Object { $_.title -eq $ExpectedTitle }).Count -ne 1) {
        throw "未识别预期风险：$ExpectedTitle，样本：$($Project.name)"
    }
    return [ordered]@{
        projectId = $Project.id
        targetJavaVersion = $Project.targetJavaVersion
        packageType = $Project.packageType
        recognizedRisk = $ExpectedTitle
    }
}

if (!(Test-Path -LiteralPath $BackendJava) -or !(Test-Path -LiteralPath $BackendJar)) {
    throw '后端包或内置 Java 运行时不存在，请先执行 build.ps1。'
}

$FullRuntimeHome = [System.IO.Path]::GetFullPath($RuntimeHome)
if (Test-Path -LiteralPath $FullRuntimeHome) {
    Remove-Item -LiteralPath $FullRuntimeHome -Recurse -Force
}
New-Item -ItemType Directory -Path $FullRuntimeHome -Force | Out-Null
$BackendOutLog = Join-Path $FullRuntimeHome 'backend.out.log'
$BackendErrLog = Join-Path $FullRuntimeHome 'backend.err.log'
$env:JARPATCH_AUTH_TOKEN = $Token
$env:JARPATCH_INSTANCE_ID = $InstanceId
$env:JAVA_HOME = $Jdk17Home
$env:Path = "$(Join-Path $Jdk17Home 'bin');$env:Path"

try {
    $BackendProcess = Start-Process -FilePath $BackendJava -ArgumentList @(
        "-Duser.home=$FullRuntimeHome",
        '-jar',
        ('"' + $BackendJar + '"'),
        "--server.port=$Port"
    ) -WorkingDirectory $FullRuntimeHome -RedirectStandardOutput $BackendOutLog `
        -RedirectStandardError $BackendErrLog -PassThru -WindowStyle Hidden

    $Ready = $false
    $ReadyDeadline = [DateTimeOffset]::Now.AddSeconds(60)
    while ([DateTimeOffset]::Now -lt $ReadyDeadline) {
        if ($BackendProcess.HasExited) {
            throw "后端在健康检查前退出：$(Get-Content -LiteralPath $BackendErrLog -Raw -ErrorAction SilentlyContinue)"
        }
        try {
            $Health = Invoke-Api 'GET' '/api/system/health'
            if ($Health.product -eq 'JarPatch Studio' -and $Health.instanceId -eq $InstanceId -and $Health.status -eq 'UP') {
                $Ready = $true
                break
            }
        }
        catch {
            Start-Sleep -Milliseconds 250
        }
    }
    if (!$Ready) {
        throw '后端健康检查超时。'
    }

    $Unauthorized = Invoke-WebRequest -Uri "$BaseUrl/api/system/health" -SkipHttpErrorCheck
    if ($Unauthorized.StatusCode -ne 401) {
        throw "无令牌请求未被拒绝，状态码：$($Unauthorized.StatusCode)"
    }

    $Java8Project = Import-Sample 'sample-java8.jar'
    $SpringBootProject = Import-Sample 'sample-spring-boot-java17.jar'
    $WarProject = Import-Sample 'sample-java8.war'
    if (($Java8Project.targetJavaVersion -ne 8) -or
            ($SpringBootProject.targetJavaVersion -ne 17) -or
            ($WarProject.targetJavaVersion -ne 8)) {
        throw '真实样本目标 Java 版本识别不符合预期。'
    }

    $Settings = Invoke-Api 'GET' "/api/projects/$($Java8Project.id)/settings"
    $Settings.defaultExportDirectory = Join-Path $OutputRoot 'exports'
    $Settings.maxEditableFileBytes = 1048576
    $SavedSettings = Invoke-Api 'PUT' "/api/projects/$($Java8Project.id)/settings" $Settings
    if ($SavedSettings.targetJavaVersion -ne 8) {
        throw '项目设置改变了原包目标 Java 版本。'
    }

    $EndToEnd = @(
        Invoke-EditCompileExport $Java8Project 'LegacyGreeting.java' 'legacy-java-8' 'legacy-java-8-patched'
        Invoke-EditCompileExport $SpringBootProject 'ModernGreeting.java' 'modern-java-17' 'modern-java-17-patched'
        Invoke-EditCompileExport $WarProject 'LegacyGreeting.java' 'legacy-java-8' 'legacy-war-java-8-patched'
    )

    $SignedProject = Import-Sample 'sample-signed.jar'
    $MultiReleaseProject = Import-Sample 'sample-multi-release.jar'
    $ObfuscatedProject = Import-Sample 'sample-obfuscated.jar'
    $RiskSamples = @(
        Assert-Risk $SignedProject '存在签名文件'
        Assert-Risk $MultiReleaseProject '存在多版本类目录'
        Assert-Risk $ObfuscatedProject '可能存在混淆代码'
    )

    $GuideItems = @(Invoke-Api 'GET' '/api/system/error-guide')
    if ($GuideItems.Count -lt 6) {
        throw '错误排查向导条目不完整。'
    }

    $CleanupPreview = Invoke-Api 'GET' "/api/projects/$($WarProject.id)/workspace/cleanup-preview"
    Invoke-Api 'DELETE' "/api/projects/$($WarProject.id)/workspace?confirmationId=$([Uri]::EscapeDataString($CleanupPreview.confirmationId))" | Out-Null
    $ProjectsAfterCleanup = @(Invoke-Api 'GET' '/api/projects')
    $CleanedProject = $ProjectsAfterCleanup | Where-Object { $_.id -eq $WarProject.id }
    if ($null -eq $CleanedProject -or [string]::IsNullOrWhiteSpace($CleanedProject.workspaceCleanedAt)) {
        throw '工作区清理后项目历史未按约定保留。'
    }

    $BackendVersionLine = (& $BackendJava -version 2>&1 |
        ForEach-Object { $_.ToString() } |
        Select-Object -First 1)
    $Result = [ordered]@{
        acceptedAt = [DateTimeOffset]::Now.ToString('o')
        backendRuntime = $BackendVersionLine
        tokenBoundary = 'unauthorized request returned HTTP 401'
        healthInstanceMatched = $true
        endToEndSamples = $EndToEnd
        riskSamples = $RiskSamples
        errorGuideItemCount = $GuideItems.Count
        workspaceCleanupPreview = [ordered]@{
            fileCount = $CleanupPreview.fileCount
            totalBytes = $CleanupPreview.totalBytes
            historyRetained = $true
        }
    }
    $ResultPath = Join-Path $OutputRoot 'acceptance-result.json'
    $Result | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $ResultPath -Encoding utf8
    Get-Content -LiteralPath $ResultPath -Raw -Encoding utf8
}
finally {
    if ($null -ne $BackendProcess -and !$BackendProcess.HasExited) {
        try {
            Invoke-Api 'POST' '/api/system/shutdown' | Out-Null
            [void]$BackendProcess.WaitForExit(5000)
        }
        catch {
        }
        if (!$BackendProcess.HasExited) {
            Stop-Process -Id $BackendProcess.Id -Force
        }
    }
    $Token = $null
}
