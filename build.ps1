param(
    [string]$JdkHome = $env:JAVA_HOME,
    [switch]$BundleJdk17,
    [switch]$Windows7,
    [switch]$RequireSigning,
    [string]$CertificateThumbprint = $env:JARPATCH_WINDOWS_CERT_THUMBPRINT,
    [string]$TimestampUrl = $env:JARPATCH_WINDOWS_TIMESTAMP_URL,
    [switch]$NativeBackend,
    [string]$NativeImageHome = $env:GRAALVM_HOME
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($PSVersionTable.PSVersion.Major -lt 7) {
    throw 'JarPatch Studio 构建必须使用 PowerShell 7 或更高版本。'
}

$ProjectRoot = [System.IO.Path]::GetFullPath($PSScriptRoot)
$FrontendRoot = Join-Path $ProjectRoot 'frontend'
$BackendArtifactName = if ($NativeBackend) { 'jarpatch-studio-backend.exe' } else { 'jarpatch-studio-backend.jar' }
$BackendTarget = if ($NativeBackend) { 'backend\target\native' } else { 'backend\target' }
$BackendArtifact = Join-Path (Join-Path $ProjectRoot $BackendTarget) $BackendArtifactName
$BackendResourceRoot = Join-Path $FrontendRoot 'backend'
$RuntimeResourceRoot = Join-Path $FrontendRoot 'runtime'
$FrontendDistRoot = Join-Path $FrontendRoot 'dist'
$ReleaseRoot = Join-Path $ProjectRoot 'release\windows'
$RootPackage = Get-Content -LiteralPath (Join-Path $ProjectRoot 'package.json') -Raw | ConvertFrom-Json
$BuildEntry = 'build.ps1'
$JdtLsVersion = '1.55.0'
$PreviousLegacyWindows = $env:JARPATCH_LEGACY_WINDOWS
$BundledJdkMajor = if ($Windows7) { 21 } elseif ($BundleJdk17 -or $NativeBackend) { 17 }
if ($Windows7 -and $NativeBackend) {
    $FrontendDistRoot = Join-Path $FrontendRoot 'dist-win7-native'
    $ReleaseRoot = Join-Path $ProjectRoot 'release\windows7-native'
}
elseif ($Windows7) {
    $FrontendDistRoot = Join-Path $FrontendRoot 'dist-win7'
    $ReleaseRoot = Join-Path $ProjectRoot 'release\windows7'
}
elseif ($NativeBackend) {
    $FrontendDistRoot = Join-Path $FrontendRoot 'dist-native'
    $ReleaseRoot = Join-Path $ProjectRoot 'release\windows-native'
}

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

function Initialize-NativeBuildEnvironment {
    if ($env:VSCMD_ARG_TGT_ARCH -in @('x64', 'amd64') -and (Get-Command 'cl.exe' -ErrorAction SilentlyContinue)) {
        return
    }
    $VsWhere = Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio\Installer\vswhere.exe'
    if (!(Test-Path -LiteralPath $VsWhere -PathType Leaf)) {
        throw '原生后端构建需要 Visual Studio 2022 Build Tools 的 MSVC x64 与 Windows SDK。'
    }
    $VsInstall = & $VsWhere -latest -products '*' -version '[17.0,18.0)' `
        -requires 'Microsoft.VisualStudio.Component.VC.Tools.x86.x64' -property installationPath
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($VsInstall)) {
        throw '未找到包含 MSVC x64 的 Visual Studio 2022。'
    }
    $DevShell = Join-Path $VsInstall 'Common7\Tools\Launch-VsDevShell.ps1'
    & $DevShell -Arch amd64 -HostArch amd64 -SkipAutomaticLocation
    if (!(Get-Command 'cl.exe' -ErrorAction SilentlyContinue)) {
        throw 'Visual Studio x64 构建环境初始化失败。'
    }
}

function Install-BundledLanguageServer {
    # 固定支持 JDK 21 的 Eclipse 版本，构建时校验官方发布包的 SHA-256。
    $ArchiveName = "jdt-language-server-$JdtLsVersion-202601131729.tar.gz"
    $ArchiveSha256 = '90627c9f03704dbb404f37651625d0751c078ab7534246023d53adcea1411b91'
    $CacheRoot = Join-Path $env:TEMP 'jarpatch-win7-dependencies'
    $ArchivePath = Join-Path $CacheRoot $ArchiveName
    if (!(Test-Path -LiteralPath $ArchivePath -PathType Leaf) -or
            (Get-FileHash -LiteralPath $ArchivePath -Algorithm SHA256).Hash -ne $ArchiveSha256) {
        New-Item -ItemType Directory -Path $CacheRoot -Force | Out-Null
        $DownloadFile = New-TemporaryFile
        try {
            Invoke-WebRequest -Uri "https://download.eclipse.org/jdtls/milestones/$JdtLsVersion/$ArchiveName" `
                -OutFile $DownloadFile.FullName -TimeoutSec 120
            if ((Get-FileHash -LiteralPath $DownloadFile.FullName -Algorithm SHA256).Hash -ne $ArchiveSha256) {
                throw 'Eclipse JDT LS 下载包的 SHA-256 不匹配。'
            }
            Move-Item -LiteralPath $DownloadFile.FullName -Destination $ArchivePath -Force
        }
        finally {
            if (Test-Path -LiteralPath $DownloadFile.FullName) {
                Remove-Item -LiteralPath $DownloadFile.FullName -Force
            }
        }
    }
    $LanguageServerRoot = Join-Path $RuntimeResourceRoot 'jdtls'
    New-Item -ItemType Directory -Path $LanguageServerRoot | Out-Null
    $Tar = Resolve-RequiredCommand 'tar.exe'
    & $Tar -xzf $ArchivePath -C $LanguageServerRoot
    if ($LASTEXITCODE -ne 0 -or !(Test-Path -LiteralPath (Join-Path $LanguageServerRoot 'config_win\config.ini') -PathType Leaf)) {
        throw '无法解压 Eclipse JDT LS Windows 配置。'
    }
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

function Invoke-PackagedSmokeCheck {
    param(
        [Parameter(Mandatory)]
        [string]$ExecutablePath
    )

    if (!(Test-Path -LiteralPath $ExecutablePath -PathType Leaf)) {
        throw "未找到 Windows 解包应用：$ExecutablePath"
    }
    $OutputFile = New-TemporaryFile
    $ErrorFile = New-TemporaryFile
    try {
        $Process = Start-Process -FilePath $ExecutablePath -ArgumentList '--smoke-check' -PassThru `
            -WindowStyle Hidden -RedirectStandardOutput $OutputFile.FullName -RedirectStandardError $ErrorFile.FullName
        if (!$Process.WaitForExit(60000)) {
            $Process.Kill($true)
            throw 'Windows 解包应用启动验收超时。'
        }
        $SmokeOutput = ((Get-Content -LiteralPath $OutputFile.FullName -Raw -ErrorAction SilentlyContinue) +
                (Get-Content -LiteralPath $ErrorFile.FullName -Raw -ErrorAction SilentlyContinue))
        if ($Process.ExitCode -ne 0 -or $SmokeOutput -notmatch '"status":"READY"') {
            throw "Windows 解包应用启动验收失败：$SmokeOutput"
        }
    }
    finally {
        Remove-Item -LiteralPath $OutputFile.FullName, $ErrorFile.FullName -Force -ErrorAction SilentlyContinue
    }
}

function Resolve-SignTool {
    $Command = Get-Command 'signtool.exe' -ErrorAction SilentlyContinue
    if ($null -ne $Command) {
        return $Command.Source
    }
    $WindowsKitsRoot = Join-Path ${env:ProgramFiles(x86)} 'Windows Kits\10\bin'
    if (!(Test-Path -LiteralPath $WindowsKitsRoot -PathType Container)) {
        throw '未找到 Windows SDK signtool.exe。'
    }
    $Candidates = Get-ChildItem -LiteralPath $WindowsKitsRoot -Filter 'signtool.exe' -File -Recurse |
        Where-Object { $_.DirectoryName -match '[\\/]x64$' } |
        Sort-Object FullName -Descending
    if ($Candidates.Count -eq 0) {
        throw '未找到 Windows SDK x64 signtool.exe。'
    }
    return $Candidates[0].FullName
}

function Invoke-CodeSigning {
    param(
        [Parameter(Mandatory)]
        [System.IO.FileInfo[]]$Executables
    )

    if ([string]::IsNullOrWhiteSpace($CertificateThumbprint)) {
        if ($RequireSigning) {
            throw '正式发布要求签名，但未设置 JARPATCH_WINDOWS_CERT_THUMBPRINT。'
        }
        return
    }
    if ([string]::IsNullOrWhiteSpace($TimestampUrl)) {
        throw '已配置签名证书，但未设置 JARPATCH_WINDOWS_TIMESTAMP_URL。'
    }
    $SignTool = Resolve-SignTool
    foreach ($Executable in $Executables) {
        & $SignTool sign '/sha1' $CertificateThumbprint '/fd' 'SHA256' '/tr' $TimestampUrl '/td' 'SHA256' $Executable.FullName
        if ($LASTEXITCODE -ne 0) {
            throw "Windows Authenticode 签名失败：$($Executable.FullName)"
        }
        $Signature = Get-AuthenticodeSignature -LiteralPath $Executable.FullName
        if ($Signature.Status -ne 'Valid') {
            throw "Windows Authenticode 验证未通过：$($Executable.FullName)，状态：$($Signature.Status)"
        }
    }
}

$Maven = Resolve-RequiredCommand 'mvn.cmd'
$Npm = Resolve-RequiredCommand 'npm.cmd'
$Node = Resolve-RequiredCommand 'node.exe'
$Git = Resolve-RequiredCommand 'git.exe'
$RequiredNodeVersion = $RootPackage.engines.node
$RequiredNpmVersion = $RootPackage.engines.npm
$ActualNodeVersion = (& $Node --version).ToString().TrimStart('v')
$ActualNpmVersion = (& $Npm --version).ToString().Trim()
if ($ActualNodeVersion -ne $RequiredNodeVersion -or $ActualNpmVersion -ne $RequiredNpmVersion) {
    throw "Node.js/npm 版本必须为 $RequiredNodeVersion/$RequiredNpmVersion，当前为 $ActualNodeVersion/$ActualNpmVersion"
}

& $Node (Join-Path $ProjectRoot 'scripts\sync-version.js') '--check'
if ($LASTEXITCODE -ne 0) {
    throw "版本一致性检查失败，退出码：$LASTEXITCODE"
}

$GitCommit = (& $Git -C $ProjectRoot rev-parse HEAD).ToString().Trim()
if ($LASTEXITCODE -ne 0) {
    throw "读取 Git commit 失败，退出码：$LASTEXITCODE"
}
$GitStatus = (& $Git -C $ProjectRoot status --porcelain --untracked-files=all | Out-String).Trim()
if ($LASTEXITCODE -ne 0) {
    throw "读取 Git 工作区状态失败，退出码：$LASTEXITCODE"
}
$SourceClean = ([string]::IsNullOrWhiteSpace($GitStatus)).ToString().ToLowerInvariant()
$BuildEntrySha256 = (Get-FileHash -LiteralPath (Join-Path $ProjectRoot $BuildEntry) -Algorithm SHA256).Hash.ToLowerInvariant()

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
$JavaMajor = [int]$Matches.major
if ($JavaMajor -lt 17) {
    throw "后端构建至少需要 JDK 17，当前版本：$JavaMajor"
}
if ($null -ne $BundledJdkMajor) {
    if ($JavaMajor -ne $BundledJdkMajor) {
        throw "当前完整 JDK 打包模式需要 JDK $BundledJdkMajor，实际版本：$JavaMajor"
    }
    $Javac = Join-Path $JdkHome 'bin\javac.exe'
    if (!(Test-Path -LiteralPath $Javac -PathType Leaf)) {
        throw "内网完整 JDK 包缺少 javac.exe：$JdkHome"
    }
    $JavacVersionOutput = (& $Javac -version 2>&1 | Out-String).Trim()
    $JavacVersionPattern = "^javac $BundledJdkMajor(?:[.\-+]|$)"
    if ($LASTEXITCODE -ne 0 -or $JavacVersionOutput -notmatch $JavacVersionPattern) {
        throw "内网完整 JDK 包编译器校验失败：$JavacVersionOutput"
    }
}
if ($Windows7) {
    $JdkRelease = Get-Content -LiteralPath (Join-Path $JdkHome 'release') -Raw
    if ($JdkRelease -notmatch '(?m)^IMPLEMENTOR="BellSoft"\r?$' -or
            $JdkRelease -notmatch '(?m)^OS_NAME="Windows"\r?$' -or
            $JdkRelease -notmatch '(?m)^OS_ARCH="x86_64"\r?$') {
        throw "Win7 兼容包必须传入 BellSoft Liberica 完整 JDK $BundledJdkMajor（Windows x64）。"
    }
}

$NativeImageVersion = $null
if ($NativeBackend) {
    if ([string]::IsNullOrWhiteSpace($NativeImageHome)) {
        throw '请通过 -NativeImageHome 或 GRAALVM_HOME 指定 GraalVM Native Image 工具链。'
    }
    $NativeImageHome = [System.IO.Path]::GetFullPath($NativeImageHome)
    $NativeImage = Join-Path $NativeImageHome 'bin\native-image.cmd'
    if (!(Test-Path -LiteralPath $NativeImage -PathType Leaf)) {
        $NativeImage = Join-Path $NativeImageHome 'bin\native-image.exe'
    }
    if (!(Test-Path -LiteralPath $NativeImage -PathType Leaf)) {
        throw "未找到 GraalVM Native Image：$NativeImageHome"
    }
    $NativeImageVersion = (& $NativeImage --version 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "GraalVM Native Image 工具链校验失败：$NativeImageVersion"
    }
    if ($Windows7 -and $NativeImageVersion -notmatch 'Liberica-NIK-23\.0\.') {
        throw 'Win7 原生包需要兼容 Win7 SP1 的 BellSoft Liberica NIK 23.0（JDK 17，Windows x64）。'
    }
    Initialize-NativeBuildEnvironment
    $NativeRuntimeRoot = if ($Windows7) {
        Join-Path $JdkHome 'bin'
    } else {
        if ([string]::IsNullOrWhiteSpace($env:VCToolsRedistDir)) {
            throw 'Visual Studio 缺少 VC x64 运行库目录。'
        }
        Join-Path $env:VCToolsRedistDir 'x64\Microsoft.VC143.CRT'
    }
    $NativeRuntimeFiles = @(
        foreach ($Library in 'vcruntime140.dll', 'vcruntime140_1.dll') {
            $LibraryPath = Join-Path $NativeRuntimeRoot $Library
            if (!(Test-Path -LiteralPath $LibraryPath -PathType Leaf)) {
                throw "缺少原生后端 VC 运行库：$LibraryPath"
            }
            Get-Item -LiteralPath $LibraryPath
        }
    )
    if ($Windows7) {
        $UcrtBase = Join-Path $NativeRuntimeRoot 'ucrtbase.dll'
        $UcrtBridge = Join-Path $NativeRuntimeRoot 'api-ms-win-crt-runtime-l1-1-0.dll'
        if (!(Test-Path -LiteralPath $UcrtBase -PathType Leaf) -or !(Test-Path -LiteralPath $UcrtBridge -PathType Leaf)) {
            throw "Win7 Liberica JDK 缺少随附的 Universal CRT：$NativeRuntimeRoot"
        }
        # 使用 Win7 版 JDK 随附的 UCRT 和 APISet，放在原生后端 EXE 旁。
        $NativeRuntimeFiles += Get-Item -LiteralPath $UcrtBase
        $NativeRuntimeFiles += Get-ChildItem -LiteralPath $NativeRuntimeRoot -Filter 'api-ms-win-*.dll' -File
    }
}

$env:JAVA_HOME = $JdkHome
$env:Path = "$(Join-Path $JdkHome 'bin');$env:Path"

Push-Location $ProjectRoot
try {
    $env:JARPATCH_LEGACY_WINDOWS = if ($Windows7) { '1' } else { $null }
    if ($NativeBackend) {
        # Native Image 工具链只用于构建，发布包内的 JDK 仍供补丁编译使用。
        $PreviousPath = $env:Path
        $PreviousGraalVmHome = $env:GRAALVM_HOME
        try {
            $env:GRAALVM_HOME = $NativeImageHome
            $env:JAVA_HOME = $NativeImageHome
            $env:Path = "$(Join-Path $NativeImageHome 'bin');$PreviousPath"
            & $Maven -pl backend '-Pnative' '-Dmaven.test.skip=true' clean package
        }
        finally {
            $env:GRAALVM_HOME = $PreviousGraalVmHome
            $env:JAVA_HOME = $JdkHome
            $env:Path = $PreviousPath
        }
    }
    else {
        & $Maven '-Dmaven.test.skip=true' package
    }
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
    $PackagedBackend = Join-Path $BackendResourceRoot $BackendArtifactName
    Copy-Item -LiteralPath $BackendArtifact -Destination $PackagedBackend
    if ($NativeBackend) {
        foreach ($Library in $NativeRuntimeFiles) {
            Copy-Item -LiteralPath $Library.FullName -Destination $BackendResourceRoot
        }
        Invoke-CodeSigning -Executables (Get-Item -LiteralPath $PackagedBackend)
    }

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
    if ($null -ne $BundledJdkMajor) {
        # 保留完整 JDK 和许可证；后端从 java.home 自动定位内置 javac，无需配置系统环境。
        Copy-Item -LiteralPath $JdkHome -Destination $RuntimeResourceRoot -Recurse
    }
    else {
        & $Jlink --add-modules $RuntimeModules --strip-debug --no-header-files --no-man-pages --compress=2 --output $RuntimeResourceRoot
        if ($LASTEXITCODE -ne 0) {
            throw "jlink 运行时生成失败，退出码：$LASTEXITCODE"
        }
    }

    Remove-BuildDirectory $FrontendDistRoot
    if ($Windows7) {
        Install-BundledLanguageServer
        $Win7ArtifactName = if ($NativeBackend) {
            'JarPatch-Studio-${version}-Windows7-Native-${arch}.${ext}'
        } else {
            'JarPatch-Studio-${version}-Windows7-${arch}.${ext}'
        }
        & $Npm --prefix $FrontendRoot run dist:win -- '--config.electronVersion=22.3.27' `
            "--config.directories.output=$FrontendDistRoot" `
            "--config.win.artifactName=$Win7ArtifactName"
    }
    elseif ($NativeBackend) {
        & $Npm --prefix $FrontendRoot run dist:win -- "--config.directories.output=$FrontendDistRoot" `
            '--config.win.artifactName=JarPatch-Studio-${version}-Windows-Native-${arch}.${ext}'
    }
    else {
        & $Npm --prefix $FrontendRoot run dist:win
    }
    if ($LASTEXITCODE -ne 0) {
        throw "Windows 发布包构建失败，退出码：$LASTEXITCODE"
    }
    $PackagedAppRoot = Join-Path $FrontendDistRoot 'win-unpacked'
    $PackagedExecutablePath = Join-Path $PackagedAppRoot 'jarpatch-studio.exe'
    if ($NativeBackend) {
        $PackagedBackendDirectory = Join-Path $PackagedAppRoot 'resources\backend'
        foreach ($FileName in (@($BackendArtifactName) + $NativeRuntimeFiles.Name)) {
            if (!(Test-Path -LiteralPath (Join-Path $PackagedBackendDirectory $FileName) -PathType Leaf)) {
                throw "原生发布包缺少后端文件：$FileName"
            }
        }
        if (Test-Path -LiteralPath (Join-Path $PackagedBackendDirectory 'jarpatch-studio-backend.jar')) {
            throw '原生发布包包含不应交付的后端 JAR。'
        }
    }
    Invoke-PackagedSmokeCheck $PackagedExecutablePath

    $PortableExecutables = @(Get-ChildItem -LiteralPath $FrontendDistRoot -Filter '*.exe' -File)
    if ($PortableExecutables.Count -ne 1) {
        throw '无法唯一确定单文件应用 EXE。'
    }
    $PortableExecutable = $PortableExecutables[0]
    $DiagnosticTarget = if ($Windows7) { 'windows7' } else { 'windows' }
    & $Node (Join-Path $ProjectRoot 'scripts\build-startup-diagnostics.js') $FrontendDistRoot $PortableExecutable.Name $DiagnosticTarget
    if ($LASTEXITCODE -ne 0) {
        throw "独立启动诊断器构建失败，退出码：$LASTEXITCODE"
    }
    $ReleaseExecutables = @(Get-ChildItem -LiteralPath $FrontendDistRoot -Filter '*.exe' -File)
    Invoke-CodeSigning -Executables ($ReleaseExecutables + (Get-Item -LiteralPath $PackagedExecutablePath))

    # 安装包复用已验收的目录；安装后直接运行，不再每次解压便携包。
    $InstallerName = "$($PortableExecutable.BaseName)-Setup.exe"
    & (Join-Path $ProjectRoot 'scripts\build-windows-installer.ps1') `
        -AppDirectory $PackagedAppRoot -OutputDirectory $FrontendDistRoot `
        -ArtifactName $InstallerName -Windows7:$Windows7
    Invoke-CodeSigning -Executables (Get-Item -LiteralPath (Join-Path $FrontendDistRoot $InstallerName))

    # 归档已验收的应用目录，签名构建中 ZIP 内的应用 EXE 也包含有效签名。
    $FastStartArchive = Join-Path $FrontendDistRoot "$($PortableExecutable.BaseName)-FastStart.zip"
    [System.IO.Compression.ZipFile]::CreateFromDirectory(
        $PackagedAppRoot, $FastStartArchive, [System.IO.Compression.CompressionLevel]::Optimal, $false)

    Remove-ReleaseDirectory
    New-Item -ItemType Directory -Path $ReleaseRoot -Force | Out-Null
    $Artifacts = Get-ChildItem -LiteralPath $FrontendDistRoot -File |
        Where-Object { $_.Extension -in @('.exe', '.blockmap', '.zip') }
    foreach ($Artifact in $Artifacts) {
        Copy-Item -LiteralPath $Artifact.FullName -Destination (Join-Path $ReleaseRoot $Artifact.Name) -Force
    }
    $SigningStatus = if ([string]::IsNullOrWhiteSpace($CertificateThumbprint)) { 'NOT_SIGNED' } else { 'VALID' }

    $JavaVersionLine = (($JavaVersionOutput -split "`r?`n")[0]).ToString()
    & $Node (Join-Path $ProjectRoot 'scripts\generate-release-manifest.js') 'windows' 'x64' $ReleaseRoot $JavaVersionLine $ActualNpmVersion $GitCommit $SourceClean $BuildEntry $BuildEntrySha256 $SigningStatus
    if ($LASTEXITCODE -ne 0) {
        throw "Windows 发布清单生成失败，退出码：$LASTEXITCODE"
    }
    if ($NativeBackend -or $Windows7) {
        $ManifestPath = Join-Path $ReleaseRoot 'release-manifest.json'
        $Manifest = Get-Content -LiteralPath $ManifestPath -Raw | ConvertFrom-Json
        if ($NativeBackend) {
            $Manifest | Add-Member -NotePropertyName 'backend' -NotePropertyValue ([ordered]@{
                mode = 'native'
                nativeImageVersion = $NativeImageVersion
            })
        }
        if ($Windows7) {
            $Manifest | Add-Member -NotePropertyName 'compatibility' -NotePropertyValue ([ordered]@{
                target = 'Windows 7 SP1 x64'
                electronVersion = '22.3.27'
                chromiumMajor = 108
                jdkVendor = 'BellSoft Liberica'
                jdtLsVersion = $JdtLsVersion
                targetOsVerified = $false
                note = '构建主机启动检查不能替代真实 Win7 验收；Electron 22 已停止维护。'
            })
        }
        $Manifest | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $ManifestPath -Encoding utf8
    }
    Write-Host "Windows 发布包已生成：$ReleaseRoot"
}
finally {
    $env:JARPATCH_LEGACY_WINDOWS = $PreviousLegacyWindows
    Pop-Location
}
