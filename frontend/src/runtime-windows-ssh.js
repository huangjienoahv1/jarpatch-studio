const { runSsh } = require('./runtime-transport');

const WINDOWS_JVM = 'windows-jvm';
const STAGING_NAME = /^jarpatch-win-arthas-[a-f0-9]{24}$/;
const COMMAND_LIMIT = 8000;
const PS_PREFIX = String.raw`
if ($PSVersionTable.PSVersion.Major -lt 7) { exit 181 }
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
if ($PSStyle) { $PSStyle.OutputRendering = 'PlainText' }
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
`;
const READ_PROCESSES = String.raw`
$filter = "Name = 'java.exe' OR Name = 'javaw.exe'"
if ($arg.pid) { $filter = "($filter) AND ProcessId = $($arg.pid)" }
$rows = @(
  foreach ($p in Get-CimInstance -ClassName Win32_Process -Filter $filter) {
    if (!$p.ExecutablePath -or !$p.CreationDate) { continue }
    $owner = Invoke-CimMethod -InputObject $p -MethodName GetOwner -ErrorAction SilentlyContinue
    $name = ''
    if ($p.CommandLine -match '(?:^|\s)-jar\s+(?:"([^"]+)"|(\S+))') {
      $name = if ($Matches[1]) { [IO.Path]::GetFileName($Matches[1]) } else { [IO.Path]::GetFileName($Matches[2]) }
    } elseif ($p.CommandLine -match '(?:^|\s)(?:-cp|-classpath|--class-path)\s+(?:"[^"]*"|\S+)\s+([A-Za-z_$][\w$.]*)(?:\s|$)') {
      $name = $Matches[1]
    }
    @{ pid = [string]$p.ProcessId; start = [string]$p.CreationDate.ToFileTimeUtc()
      executable = $p.ExecutablePath; user = $owner.User; name = $name }
  }
)
ConvertTo-Json -InputObject $rows -Compress
`;
const CHECK_PROCESS = String.raw`
$p = Get-CimInstance -ClassName Win32_Process -Filter "ProcessId = $($arg.pid)"
if (!$p -or !$p.CreationDate -or [string]$p.CreationDate.ToFileTimeUtc() -ne $arg.start -or
    $p.ExecutablePath -ne $arg.executable) { exit 191 }
$owner = Invoke-CimMethod -InputObject $p -MethodName GetOwnerSid
if ($owner.ReturnValue -ne 0 -or $owner.Sid -ne [Security.Principal.WindowsIdentity]::GetCurrent().User.Value) { exit 182 }
`;
const STAGING_PATH = String.raw`
if ($arg.staging -cnotmatch '^jarpatch-win-arthas-[a-f0-9]{24}$') { exit 198 }
$root = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\') + '\'
$dir = [IO.Path]::GetFullPath([IO.Path]::Combine($root, $arg.staging))
if (!$dir.StartsWith($root, [StringComparison]::OrdinalIgnoreCase)) { exit 198 }
$item = Get-Item -LiteralPath $dir -Force -ErrorAction SilentlyContinue
if ($item -and (!$item.PSIsContainer -or ($item.Attributes -band [IO.FileAttributes]::ReparsePoint))) { exit 198 }
`;
const FIND_JDK = String.raw`
$bin = [IO.Path]::GetDirectoryName($p.ExecutablePath)
$jdkHome = [IO.Path]::GetDirectoryName($bin)
if ([IO.Path]::GetFileName($jdkHome) -ieq 'jre') {
  $jdkHome = [IO.Path]::GetDirectoryName($jdkHome)
  $bin = [IO.Path]::Combine($jdkHome, 'bin')
}
$java = [IO.Path]::Combine($bin, 'java.exe')
$jar = [IO.Path]::Combine($bin, 'jar.exe')
if (![IO.File]::Exists($java) -or ![IO.File]::Exists($jar)) { exit 194 }
`;
const COPY_STDIN = String.raw`
function Copy-InputFile($location) {
  $file = [IO.File]::Open($location, [IO.FileMode]::CreateNew)
  try { [Console]::OpenStandardInput().CopyTo($file) } finally { $file.Dispose() }
}
`;
const UPLOAD = CHECK_PROCESS + STAGING_PATH + FIND_JDK + COPY_STDIN + String.raw`
if ($item) { exit 198 }
[IO.Directory]::CreateDirectory($dir) | Out-Null
$archive = [IO.Path]::Combine($dir, 'package.zip')
Copy-InputFile $archive
if ((Get-Item -LiteralPath $archive).Length -ne $arg.size -or
    (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash -ne $arg.sha256) { exit 184 }
Set-Location -LiteralPath $dir
& $jar xf $archive
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Remove-Item -LiteralPath $archive
`;
const BOOT = CHECK_PROCESS + STAGING_PATH + FIND_JDK + COPY_STDIN + String.raw`
if (!$item) { exit 198 }
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, [int]$arg.port)
try { $listener.Start() } catch { exit 195 } finally { $listener.Stop() }
$configuration = [IO.Path]::Combine($dir, 'arthas.properties')
[IO.File]::Delete($configuration)
Copy-InputFile $configuration
$core = [IO.Path]::Combine($dir, 'arthas-core.jar')
$agent = [IO.Path]::Combine($dir, 'arthas-agent.jar')
$tools = [IO.Path]::Combine($jdkHome, 'lib', 'tools.jar')
$flags = @()
if ([IO.File]::Exists($tools)) { $flags += "-Xbootclasspath/a:$tools" }
Set-Location -LiteralPath $dir
& $java @flags -jar $core -pid $arg.pid -target-ip 127.0.0.1 -telnet-port -1 -http-port $arg.port -session-timeout 120 -core $core -agent $agent
exit $LASTEXITCODE
`;
const REMOVE_FILES = String.raw`
$agentFiles = @('arthas-agent.jar', 'arthas-core.jar', 'arthas-spy.jar')
$marker = '.pending-cleanup'
function Remove-ArthasFiles($location, $sweep) {
  if (![IO.Directory]::Exists($location)) { return $false }
  if ($sweep -and ![IO.File]::Exists([IO.Path]::Combine($location, $marker))) { return $false }
  [IO.File]::Delete([IO.Path]::Combine($location, 'arthas.properties'))
  $deferred = $false
  foreach ($entry in Get-ChildItem -LiteralPath $location -Force) {
    if ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Arthas temporary path is a reparse point' }
    if ($sweep -and $entry.Name -notin ($agentFiles + $marker)) { throw 'Unexpected Arthas temporary file' }
    if ($entry.PSIsContainer) {
      $child = [IO.Path]::GetFullPath($entry.FullName)
      $prefix = [IO.Path]::GetFullPath($location).TrimEnd('\') + '\'
      if (!$child.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) { throw 'Invalid Arthas temporary path' }
      Remove-Item -LiteralPath $child -Recurse -Force
      continue
    }
    try { [IO.File]::Delete($entry.FullName) } catch {
      $cause = $_.Exception.GetBaseException()
      if ($entry.Name -notin $agentFiles -or (($cause.HResult -band 65535) -notin (32, 33))) { throw }
      $deferred = $true
    }
  }
  if ($deferred) {
    [IO.File]::WriteAllText([IO.Path]::Combine($location, $marker), '')
  } else {
    [IO.Directory]::Delete($location)
  }
  return $deferred
}
`;
const REMOVE = STAGING_PATH + REMOVE_FILES + String.raw`
if (Remove-ArthasFiles $dir $false) { 'deferred' }
`;
const SWEEP = REMOVE_FILES + String.raw`
$root = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
foreach ($entry in Get-ChildItem -LiteralPath $root -Directory -Filter 'jarpatch-win-arthas-*') {
  if ($entry.Name -cmatch '^jarpatch-win-arthas-[a-f0-9]{24}$' -and
      !($entry.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
    Remove-ArthasFiles $entry.FullName $true | Out-Null
  }
}
`;
const CHECK_PORT = CHECK_PROCESS + String.raw`
$client = [Net.Sockets.TcpClient]::new()
try {
  $client.Connect([Net.IPAddress]::Loopback, [int]$arg.port)
  'open'
} catch {
  $cause = $_.Exception.GetBaseException()
  if ($cause.SocketErrorCode -ne [Net.Sockets.SocketError]::ConnectionRefused) { throw }
  'closed'
} finally {
  $client.Dispose()
}
`;

async function runWindows(host, script, parameters, options = {}) {
  const encodedParameters = Buffer.from(JSON.stringify(parameters)).toString('base64');
  const decoder = "$arg = ConvertFrom-Json -InputObject ([Text.Encoding]::UTF8.GetString([Convert]::FromBase64String('"
    + encodedParameters + "')))\n";
  const source = PS_PREFIX + "try {\n" + decoder + script
    + "\n} catch { [Console]::Error.WriteLine($_.Exception.GetBaseException().Message); exit 1 }\n";
  const command = 'pwsh.exe -NoLogo -NoProfile -NonInteractive -EncodedCommand '
    + Buffer.from(source, 'utf16le').toString('base64');
  if (command.length > COMMAND_LIMIT) throw new Error('Windows SSH 操作参数过长');
  try {
    return await runSsh(host, command, options);
  } catch (error) {
    if (/^远端操作退出码 (127|181|9009)\b/.test(error.message) || /\bpwsh(?:\.exe)?.*not recognized/i.test(error.message)) {
      throw new Error('Windows 宿主机需要将 PowerShell 7 的 pwsh.exe 加入 SSH 账号的 PATH');
    }
    if (/^远端操作退出码 182\b/.test(error.message)) throw new Error('请使用运行目标 JVM 的 Windows 账号通过 SSH 连接');
    if (/^远端操作退出码 184\b/.test(error.message)) throw new Error('Windows 宿主机上的 Arthas 上传包校验失败');
    throw error;
  }
}

async function windowsProcesses(host, signal, pid) {
  if (pid != null && (typeof pid !== 'string' || !/^[1-9]\d{0,9}$/.test(pid))) throw new Error('Java 进程号无效');
  const result = JSON.parse((await runWindows(host, READ_PROCESSES, { pid }, { signal })).trim());
  if (!Array.isArray(result)) throw new Error('Windows 宿主机未返回 Java 进程列表');
  return result.filter((row) => /^[1-9]\d{0,9}$/.test(row.pid) && /^\d+$/.test(row.start)
    && /^[a-z]:\\.+\\javaw?\.exe$/i.test(row.executable));
}

function stagingParameters(session) {
  if (!STAGING_NAME.test(session.staging)) throw new Error('Windows Arthas 临时目录无法确认');
  return { pid: session.pid, start: session.start, executable: session.executable, staging: session.staging, port: session.port };
}

function uploadWindowsArthas(session, input, sha256, signal) {
  return runWindows(session.host, UPLOAD, { ...stagingParameters(session), size: input.length, sha256 },
    { input, signal, timeout: 60000 });
}

function startWindowsArthas(session, properties) {
  return runWindows(session.host, BOOT, stagingParameters(session),
    { input: properties, timeout: 42000, sensitive: true, bootDiagnostics: true });
}

async function removeWindowsArthas(session) {
  const output = await runWindows(session.host, REMOVE, { staging: stagingParameters(session).staging }, { timeout: 15000 });
  return output.trim() === 'deferred';
}

async function sweepWindowsArthas(host, signal) {
  try {
    await runWindows(host, SWEEP, {}, { signal });
  } catch (error) {
    if (signal.aborted) throw error;
    console.warn('回收 Windows 宿主机的 Arthas 缓存未确认：' + (error.message.match(/^远端操作退出码 \d+/)?.[0] || error.name));
  }
}

async function windowsArthasListening(session) {
  try {
    const result = (await runWindows(session.host, CHECK_PORT, stagingParameters(session), { timeout: 6000 })).trim();
    if (result === 'open' || result === 'closed') return result === 'open';
    throw new Error('Windows 宿主机未返回可确认的 Arthas 端口状态');
  } catch (error) {
    if (/^远端操作退出码 191\b/.test(error.message)) return false;
    throw error;
  }
}

module.exports = { WINDOWS_JVM, windowsProcesses, uploadWindowsArthas, startWindowsArthas, removeWindowsArthas,
  sweepWindowsArthas, windowsArthasListening };
