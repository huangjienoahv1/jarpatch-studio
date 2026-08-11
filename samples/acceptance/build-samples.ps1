param(
    [string]$Jdk8Home = 'C:\Program Files\Java\jdk1.8.0_201',
    [string]$Jdk17Home = 'C:\Program Files\Java\jdk-17'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($PSVersionTable.PSVersion.Major -lt 7) {
    throw '验收样本必须使用 PowerShell 7 或更高版本构建。'
}

$SampleRoot = [System.IO.Path]::GetFullPath($PSScriptRoot)
$OutputRoot = Join-Path $SampleRoot 'output'
$WorkRoot = Join-Path $SampleRoot 'work'

function Reset-SampleDirectory {
    param(
        [Parameter(Mandatory)]
        [string]$Path
    )

    $FullPath = [System.IO.Path]::GetFullPath($Path)
    if (!$FullPath.StartsWith($SampleRoot + [System.IO.Path]::DirectorySeparatorChar,
            [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "拒绝清理验收样本目录以外的路径：$FullPath"
    }
    if (Test-Path -LiteralPath $FullPath) {
        Remove-Item -LiteralPath $FullPath -Recurse -Force
    }
    New-Item -ItemType Directory -Path $FullPath -Force | Out-Null
}

function Invoke-BuildTool {
    param(
        [Parameter(Mandatory)]
        [string]$Tool,
        [Parameter(Mandatory)]
        [string[]]$Arguments
    )

    & $Tool @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "构建工具执行失败：$Tool，退出码：$LASTEXITCODE"
    }
}

$Javac8 = Join-Path $Jdk8Home 'bin\javac.exe'
$Jar8 = Join-Path $Jdk8Home 'bin\jar.exe'
$Javac17 = Join-Path $Jdk17Home 'bin\javac.exe'
$Jar17 = Join-Path $Jdk17Home 'bin\jar.exe'
$Keytool = Join-Path $Jdk17Home 'bin\keytool.exe'
$Jarsigner = Join-Path $Jdk17Home 'bin\jarsigner.exe'
foreach ($Tool in @($Javac8, $Jar8, $Javac17, $Jar17, $Keytool, $Jarsigner)) {
    if (!(Test-Path -LiteralPath $Tool)) {
        throw "验收样本构建工具不存在：$Tool"
    }
}
Reset-SampleDirectory $OutputRoot
Reset-SampleDirectory $WorkRoot

$Java8Classes = Join-Path $WorkRoot 'java8-classes'
New-Item -ItemType Directory -Path $Java8Classes -Force | Out-Null
Invoke-BuildTool $Javac8 @('-encoding', 'UTF-8', '-d', $Java8Classes,
    (Join-Path $SampleRoot 'src\java8\sample\LegacyGreeting.java'))
$MainManifest = Join-Path $WorkRoot 'main-manifest.mf'
@('Manifest-Version: 1.0', 'Main-Class: sample.LegacyGreeting', '') |
    Set-Content -LiteralPath $MainManifest -Encoding ascii
$LegacyJar = Join-Path $OutputRoot 'sample-java8.jar'
Invoke-BuildTool $Jar8 @('cfm', $LegacyJar, $MainManifest, '-C', $Java8Classes, '.')

$WarRoot = Join-Path $WorkRoot 'war-root'
$WarClasses = Join-Path $WarRoot 'WEB-INF\classes\sample'
New-Item -ItemType Directory -Path $WarClasses -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $Java8Classes 'sample\LegacyGreeting.class') -Destination $WarClasses -Force
New-Item -ItemType Directory -Path (Join-Path $WarRoot 'WEB-INF') -Force | Out-Null
@('<?xml version="1.0" encoding="UTF-8"?>', '<web-app version="3.1"></web-app>') |
    Set-Content -LiteralPath (Join-Path $WarRoot 'WEB-INF\web.xml') -Encoding utf8
Invoke-BuildTool $Jar8 @('cf', (Join-Path $OutputRoot 'sample-java8.war'), '-C', $WarRoot, '.')

$Java17Classes = Join-Path $WorkRoot 'java17-classes'
$BootRoot = Join-Path $WorkRoot 'spring-boot-root'
$BootClasses = Join-Path $BootRoot 'BOOT-INF\classes'
New-Item -ItemType Directory -Path $Java17Classes, $BootClasses -Force | Out-Null
Invoke-BuildTool $Javac17 @('--release', '17', '-encoding', 'UTF-8', '-d', $Java17Classes,
    (Join-Path $SampleRoot 'src\java17\sample\ModernGreeting.java'))
Get-ChildItem -LiteralPath $Java17Classes | Copy-Item -Destination $BootClasses -Recurse -Force
$BootManifest = Join-Path $WorkRoot 'spring-boot-manifest.mf'
@('Manifest-Version: 1.0',
    'Main-Class: org.springframework.boot.loader.launch.JarLauncher',
    'Start-Class: sample.ModernGreeting',
    'Spring-Boot-Classes: BOOT-INF/classes/',
    'Spring-Boot-Lib: BOOT-INF/lib/',
    '') | Set-Content -LiteralPath $BootManifest -Encoding ascii
Invoke-BuildTool $Jar17 @('cfm', (Join-Path $OutputRoot 'sample-spring-boot-java17.jar'),
    $BootManifest, '-C', $BootRoot, '.')

$MultiBaseClasses = Join-Path $WorkRoot 'multi-base'
$Multi17Classes = Join-Path $WorkRoot 'multi-17'
$MultiRoot = Join-Path $WorkRoot 'multi-root'
New-Item -ItemType Directory -Path $MultiBaseClasses, $Multi17Classes, $MultiRoot -Force | Out-Null
Invoke-BuildTool $Javac8 @('-encoding', 'UTF-8', '-d', $MultiBaseClasses,
    (Join-Path $SampleRoot 'src\multirelease\base\sample\VersionedGreeting.java'))
Invoke-BuildTool $Javac17 @('--release', '17', '-encoding', 'UTF-8', '-d', $Multi17Classes,
    (Join-Path $SampleRoot 'src\multirelease\java17\sample\VersionedGreeting.java'))
Get-ChildItem -LiteralPath $MultiBaseClasses | Copy-Item -Destination $MultiRoot -Recurse -Force
$MultiVersionRoot = Join-Path $MultiRoot 'META-INF\versions\17'
New-Item -ItemType Directory -Path $MultiVersionRoot -Force | Out-Null
Get-ChildItem -LiteralPath $Multi17Classes | Copy-Item -Destination $MultiVersionRoot -Recurse -Force
$MultiManifest = Join-Path $WorkRoot 'multi-manifest.mf'
@('Manifest-Version: 1.0', 'Multi-Release: true', '') | Set-Content -LiteralPath $MultiManifest -Encoding ascii
Invoke-BuildTool $Jar17 @('cfm', (Join-Path $OutputRoot 'sample-multi-release.jar'), $MultiManifest,
    '-C', $MultiRoot, '.')

$ObfuscatedClasses = Join-Path $WorkRoot 'obfuscated-classes'
New-Item -ItemType Directory -Path $ObfuscatedClasses -Force | Out-Null
$ObfuscatedSources = Get-ChildItem -LiteralPath (Join-Path $SampleRoot 'src\obfuscated') -Filter '*.java' |
    Sort-Object Name | Select-Object -ExpandProperty FullName
Invoke-BuildTool $Javac8 (@('-encoding', 'UTF-8', '-d', $ObfuscatedClasses) + $ObfuscatedSources)
Invoke-BuildTool $Jar8 @('cf', (Join-Path $OutputRoot 'sample-obfuscated.jar'), '-C', $ObfuscatedClasses, '.')

$SignedJar = Join-Path $OutputRoot 'sample-signed.jar'
Copy-Item -LiteralPath $LegacyJar -Destination $SignedJar -Force
$KeyStore = Join-Path $WorkRoot 'acceptance-keystore.p12'
$TemporaryPassword = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
try {
    Invoke-BuildTool $Keytool @('-genkeypair', '-alias', 'acceptance', '-keyalg', 'RSA', '-keysize', '2048',
        '-validity', '1', '-storetype', 'PKCS12', '-keystore', $KeyStore, '-storepass', $TemporaryPassword,
        '-keypass', $TemporaryPassword, '-dname', 'CN=JarPatch Studio Acceptance')
    Invoke-BuildTool $Jarsigner @('-keystore', $KeyStore, '-storepass', $TemporaryPassword,
        '-keypass', $TemporaryPassword, $SignedJar, 'acceptance')
}
finally {
    Remove-Item -LiteralPath $KeyStore -Force -ErrorAction SilentlyContinue
    $TemporaryPassword = $null
}

Get-ChildItem -LiteralPath $OutputRoot -File | Select-Object Name, Length
