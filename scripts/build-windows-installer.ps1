param(
    [Parameter(Mandatory)]
    [string]$AppDirectory,
    [Parameter(Mandatory)]
    [string]$OutputDirectory,
    [string]$ArtifactName = 'JarPatch-Studio-${version}-Windows-${arch}-Setup.${ext}',
    [switch]$Windows7
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if ($PSVersionTable.PSVersion.Major -lt 7) {
    throw '安装包构建必须使用 PowerShell 7 或更高版本。'
}
$ProjectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$FrontendRoot = Join-Path $ProjectRoot 'frontend'
$AppDirectory = [System.IO.Path]::GetFullPath($AppDirectory)
$OutputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
if (!(Test-Path -LiteralPath (Join-Path $AppDirectory 'jarpatch-studio.exe') -PathType Leaf) -or
        !(Test-Path -LiteralPath (Join-Path $AppDirectory 'resources/app.asar') -PathType Leaf)) {
    throw "应用目录不完整：$AppDirectory"
}
if ($OutputDirectory -eq $AppDirectory -or
        $OutputDirectory.StartsWith($AppDirectory + [System.IO.Path]::DirectorySeparatorChar,
            [System.StringComparison]::OrdinalIgnoreCase)) {
    throw '安装包输出目录不能位于待打包应用目录内。'
}
if ($ArtifactName -match '[/\\]' -or $ArtifactName -notmatch '\.(exe|\$\{ext\})$') {
    throw '安装包名称必须是不含目录的 EXE 文件名。'
}
$Builder = Join-Path $FrontendRoot 'node_modules/.bin/electron-builder.cmd'
if (!(Test-Path -LiteralPath $Builder -PathType Leaf)) {
    throw '未找到 electron-builder，请先在 frontend 执行 npm ci。'
}
$BuilderArguments = @(
    '--config', (Join-Path $FrontendRoot 'electron-builder-installer.cjs'),
    '--prepackaged', $AppDirectory, '--win', 'nsis', '--x64',
    "--config.directories.output=$OutputDirectory",
    "--config.nsis.artifactName=$ArtifactName"
)
if ($Windows7) {
    $BuilderArguments += '--config.electronVersion=22.3.27'
}
Push-Location $FrontendRoot
try {
    & $Builder @BuilderArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Windows 安装包构建失败，退出码：$LASTEXITCODE"
    }
}
finally {
    Pop-Location
}
