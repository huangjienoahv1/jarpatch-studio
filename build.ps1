param(
    [string]$JdkHome = $env:JAVA_HOME
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($PSVersionTable.PSVersion.Major -lt 7) {
    throw 'JarPatch Studio 构建必须使用 PowerShell 7 或更高版本。'
}

$ProjectRoot = [System.IO.Path]::GetFullPath($PSScriptRoot)
$FrontendRoot = Join-Path $ProjectRoot 'frontend'
$BackendJar = Join-Path $ProjectRoot 'backend\target\jarpatch-studio-backend.jar'
$BackendResourceRoot = Join-Path $FrontendRoot 'backend'
$RuntimeResourceRoot = Join-Path $FrontendRoot 'runtime'
$FrontendDistRoot = Join-Path $FrontendRoot 'dist'
$ReleaseRoot = Join-Path $ProjectRoot 'release\windows'
$RootPackage = Get-Content -LiteralPath (Join-Path $ProjectRoot 'package.json') -Raw | ConvertFrom-Json

function Resolve-RequiredCommand {
    param(
        [Parameter(Mandatory)]
        [string]$Name
    )

    $Command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($null -eq $Command) {
        throw "未找到必需命令：$Name"
    }
    return $Command.Source
}

function Remove-BuildDirectory {
    param(
        [Parameter(Mandatory)]
        [string]$Path
    )

    $FullPath = [System.IO.Path]::GetFullPath($Path)
    $FullFrontendRoot = [System.IO.Path]::GetFullPath($FrontendRoot)
    if (!$FullPath.StartsWith($FullFrontendRoot + [System.IO.Path]::DirectorySeparatorChar,
            [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "拒绝清理前端目录以外的路径：$FullPath"
    }
    if (Test-Path -LiteralPath $FullPath) {
        Remove-Item -LiteralPath $FullPath -Recurse -Force
    }
}

function Remove-ReleaseDirectory {
    $FullPath = [System.IO.Path]::GetFullPath($ReleaseRoot)
    $AllowedRoot = [System.IO.Path]::GetFullPath((Join-Path $ProjectRoot 'release'))
    if (!$FullPath.StartsWith($AllowedRoot + [System.IO.Path]::DirectorySeparatorChar,
            [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "拒绝清理发布目录以外的路径：$FullPath"
    }
    if (Test-Path -LiteralPath $FullPath) {
        Remove-Item -LiteralPath $FullPath -Recurse -Force
    }
}

$Maven = Resolve-RequiredCommand 'mvn.cmd'
$Npm = Resolve-RequiredCommand 'npm.cmd'
$Node = Resolve-RequiredCommand 'node.exe'
$RequiredNodeVersion = $RootPackage.engines.node
$RequiredNpmVersion = $RootPackage.engines.npm
$ActualNodeVersion = (& $Node --version).ToString().TrimStart('v')
$ActualNpmVersion = (& $Npm --version).ToString().Trim()
if ($ActualNodeVersion -ne $RequiredNodeVersion -or $ActualNpmVersion -ne $RequiredNpmVersion) {
    throw "Node.js/npm 版本必须为 $RequiredNodeVersion/$RequiredNpmVersion，当前为 $ActualNodeVersion/$ActualNpmVersion"
}

if ([string]::IsNullOrWhiteSpace($JdkHome)) {
    $JavaCommand = Resolve-RequiredCommand 'java.exe'
    $JdkHome = Split-Path (Split-Path $JavaCommand -Parent) -Parent
}
$JdkHome = [System.IO.Path]::GetFullPath($JdkHome)
$Java = Join-Path $JdkHome 'bin\java.exe'
$Jlink = Join-Path $JdkHome 'bin\jlink.exe'
if (!(Test-Path -LiteralPath $Java) -or !(Test-Path -LiteralPath $Jlink)) {
    throw "JDK 不完整，必须同时包含 java.exe 和 jlink.exe：$JdkHome"
}

$JavaVersionOutput = (& $Java -version 2>&1 | Out-String)
if ($JavaVersionOutput -notmatch 'version "(?<major>\d+)') {
    throw "无法识别 JDK 版本：$JavaVersionOutput"
}
if ([int]$Matches.major -lt 17) {
    throw "后端构建至少需要 JDK 17，当前版本：$($Matches.major)"
}

$env:JAVA_HOME = $JdkHome
$env:Path = "$(Join-Path $JdkHome 'bin');$env:Path"

Push-Location $ProjectRoot
try {
    & $Maven '-Dmaven.test.skip=true' package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven 后端构建失败，退出码：$LASTEXITCODE"
    }

    & $Npm --prefix $FrontendRoot ci
    if ($LASTEXITCODE -ne 0) {
        throw "前端依赖安装失败，退出码：$LASTEXITCODE"
    }

    Remove-BuildDirectory $BackendResourceRoot
    Remove-BuildDirectory $RuntimeResourceRoot
    New-Item -ItemType Directory -Path $BackendResourceRoot -Force | Out-Null
    Copy-Item -LiteralPath $BackendJar -Destination (Join-Path $BackendResourceRoot 'jarpatch-studio-backend.jar')

    $RuntimeModules = @(
        'java.base',
        'java.compiler',
        'java.desktop',
        'java.instrument',
        'java.logging',
        'java.management',
        'java.naming',
        'java.net.http',
        'java.security.jgss',
        'java.sql',
        'java.transaction.xa',
        'java.xml',
        'jdk.crypto.ec',
        'jdk.unsupported',
        'jdk.zipfs'
    ) -join ','
    & $Jlink --add-modules $RuntimeModules --strip-debug --no-header-files --no-man-pages --compress=2 --output $RuntimeResourceRoot
    if ($LASTEXITCODE -ne 0) {
        throw "jlink 运行时生成失败，退出码：$LASTEXITCODE"
    }

    Remove-BuildDirectory $FrontendDistRoot
    & $Npm --prefix $FrontendRoot run dist:win
    if ($LASTEXITCODE -ne 0) {
        throw "Windows 发布包构建失败，退出码：$LASTEXITCODE"
    }

    Remove-ReleaseDirectory
    New-Item -ItemType Directory -Path $ReleaseRoot -Force | Out-Null
    $Artifacts = Get-ChildItem -LiteralPath (Join-Path $FrontendRoot 'dist') -File |
        Where-Object { $_.Extension -in @('.exe', '.blockmap') }
    foreach ($Artifact in $Artifacts) {
        Copy-Item -LiteralPath $Artifact.FullName -Destination (Join-Path $ReleaseRoot $Artifact.Name) -Force
    }

    $JavaVersionLine = (($JavaVersionOutput -split "`r?`n")[0]).ToString()
    & $Node (Join-Path $ProjectRoot 'scripts\generate-release-manifest.js') 'windows' 'x64' $ReleaseRoot $JavaVersionLine $ActualNpmVersion
    if ($LASTEXITCODE -ne 0) {
        throw "Windows 发布清单生成失败，退出码：$LASTEXITCODE"
    }
    Write-Host "Windows 发布包已生成：$ReleaseRoot"
}
finally {
    Pop-Location
}
