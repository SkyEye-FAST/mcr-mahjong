param(
    [string]$Upstream = '.reference/upstream',
    [string]$Oracle = '.reference/oracle/oracle.exe'
)
$ErrorActionPreference = 'Stop'
$revision = & git -C $Upstream rev-parse HEAD
if ($LASTEXITCODE -ne 0 -or $revision -ne '44a178af08bf11f82a8993fddbe2fe8876ddd8f3') {
    throw 'Use the upstream revision in NOTICE.'
}
$requests = @(
    'S|111m5m12p1569sSWP', 'S|[111m]5m12p1569sSWP',
    'S|19m19s199pESWNCF', 'S|19m19s19pESWNCFP', 'S|2229999mSSWWFF',
    'S|369s147pESWNCFP', 'S|58m369s17pEWNCFP', 'S|258m369s147pECFP',
    'S|1112345678999s', 'S|1112223456777m', 'S|2223334445678m',
    'S|25558m369s46778p', 'S|25558m369s14677p', 'S|25568m369s14777p',
    'S|258m369s1445677p', 'S|2233445566778s', 'S|[111m]2458m369s147p',
    'S|22334455p77779s',
    'D|19m19s19pESWNCFPN', 'D|[111m]5m12p1569sSWP2m',
    'D|2229999mSSWWFFF', 'D|258m369s147pEECCC',
    'D|11223344556677m', 'D|[111m][123s][456p][EEE]2m3m'
)
$start = [System.Diagnostics.ProcessStartInfo]::new((Resolve-Path $Oracle))
$start.UseShellExecute = $false
$start.RedirectStandardInput = $true
$start.RedirectStandardOutput = $true
$process = [System.Diagnostics.Process]::Start($start)
$sha = [System.Security.Cryptography.SHA256]::Create()
try {
    '# C++ revision 44a178af08bf11f82a8993fddbe2fe8876ddd8f3; MIT, see NOTICE.'
    '# S: all five shanten/useful tables and waiting table in Tile enum order.'
    '# D: SHA-256 of the entire ordered discard/form/shanten/useful response (UTF-8, no newline).'
    foreach ($request in $requests) {
        $process.StandardInput.WriteLine($request)
        $process.StandardInput.Flush()
        $response = $process.StandardOutput.ReadLine()
        if (!$response -or $response.StartsWith('PARSE:')) { throw "$request : $response" }
        if ($request.StartsWith('D|')) {
            $hash = $sha.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($response))
            $response = [System.BitConverter]::ToString($hash).Replace('-', '').ToLowerInvariant()
        }
        "$request`t$response"
    }
} finally {
    $sha.Dispose()
    $process.StandardInput.Close()
    if (!$process.WaitForExit(3000)) { $process.Kill() }
    $process.Dispose()
}
