param(
    [string]$Upstream = '.reference/upstream',
    [string]$Oracle = '.reference/oracle/oracle.exe'
)
$ErrorActionPreference = 'Stop'
$revision = & git -C $Upstream rev-parse HEAD
if ($LASTEXITCODE -ne 0 -or $revision -ne '44a178af08bf11f82a8993fddbe2fe8876ddd8f3') {
    throw 'The oracle source checkout must match the revision in NOTICE.'
}
$fanSource = Get-Content 'src/main/kotlin/top/skyeyefast/mcr/internal/UpstreamFan.kt' -Raw -Encoding UTF8
$names = @([regex]::Matches($fanSource, '\b([A-Z][A-Z_]+)\(\d+\)').ForEach({ $_.Groups[1].Value }))
$source = Get-Content "$Upstream/unit_test.cpp" -Raw
$pattern = '(?m)^\s*test_points\("([^"]+)",\s*([^,]+),\s*wind_t::(\w+),\s*wind_t::(\w+)\)'
$flagValues = @{ WIN_FLAG_DISCARD=0; WIN_FLAG_SELF_DRAWN=1; WIN_FLAG_LAST_TILE=2; WIN_FLAG_KONG_INVOLVED=4; WIN_FLAG_WALL_LAST=8; WIN_FLAG_INITIAL=16 }
$winds = @{ EAST=0; SOUTH=1; WEST=2; NORTH=3 }
$requests = [System.Collections.Generic.List[string]]::new()
foreach ($match in [regex]::Matches($source, $pattern)) {
    $flags = 0
    foreach ($name in $match.Groups[2].Value.Split('|')) { $flags = $flags -bor $flagValues[$name.Trim()] }
    $requests.Add("F|$($match.Groups[1].Value)|$flags|$($winds[$match.Groups[3].Value])|$($winds[$match.Groups[4].Value])|0")
}
# Explicit flag corrections, mixed-kong compatibility, flowers, non-wins.
$requests.Add('F|[123m][444s][789m]34pCC2p|4|1|3|0')
$requests.Add('F|[123m][444s][789m]34pCC2p|8|1|3|0')
$requests.Add('F|[123m][444s][789m]34pCC2p|9|1|3|8')
$requests.Add('F|[1111m][2222s1]345pEE67s8s|1|0|0|0')
$requests.Add('F|123m456s789pEE23m5m|0|0|0|0')
$requests.Add('F|[1111m][2222s]345pEE67s8s|1|0|0|0')
$requests.Add('F|[123m][444s][789m]34pCC2p|0|1|3|8')
$start = [System.Diagnostics.ProcessStartInfo]::new((Resolve-Path $Oracle))
$start.UseShellExecute = $false
$start.RedirectStandardInput = $true
$start.RedirectStandardOutput = $true
$process = [System.Diagnostics.Process]::Start($start)
try {
    $rows = @(
        foreach ($request in $requests) {
            $process.StandardInput.WriteLine($request)
            $process.StandardInput.Flush()
            $response = $process.StandardOutput.ReadLine()
            if (!$response -or $response.StartsWith('PARSE:')) { throw "$request : $response" }
            $numbers = @($response.Split(',').ForEach({ [int]$_ }))
            if ($numbers.Count -ne $names.Count + 1) { throw 'Fan table size mismatch' }
            $fans = @(
                for ($i = 0; $i -lt $names.Count; $i++) {
                    if ($numbers[$i + 1] -gt 0) { "$($names[$i])=$($numbers[$i + 1])" }
                }
            )
            [pscustomobject]@{ Request=$request; Result="$($numbers[0]);$($fans -join ',')"; Fans=$fans.ForEach({ $_.Split('=')[0] }) }
        }
    )
    $selected = [System.Collections.Generic.List[object]]::new()
    $covered = [System.Collections.Generic.HashSet[string]]::new()
    # Keep recent upstream bug regressions, then greedily cover remaining fans.
    foreach ($row in ($rows | Select-Object -First 15)) {
        $selected.Add($row)
        foreach ($fan in $row.Fans) { [void]$covered.Add($fan) }
    }
    while ($true) {
        $best = $null; $gain = 0
        foreach ($row in $rows) {
            $new = @($row.Fans | Where-Object { !$covered.Contains($_) }).Count
            if ($new -gt $gain) { $best = $row; $gain = $new }
        }
        if ($gain -eq 0) { break }
        $selected.Add($best)
        foreach ($fan in $best.Fans) { [void]$covered.Add($fan) }
    }
    # Preserve high-value historical regressions even when they add no new fan.
    $regressions = @(
        '2222444466688m3m', '1233369m147s258p3m', '2333469m147s258p3m',
        '3369m147s258pEEE3m', '1223358m147s369p3m', '2233458m147s369p3m',
        '2358m147s369pEEE3m', '1122233334444s2s', '33469m258s147pWW2m',
        '234s2233445678p8p', '445566m2277779s8s', 'EESSWWNNCCFFPP',
        '[2222s][3333s][5555p1]67mEE8m', '1112345678999p9p',
        '112233456789mEE', '123445566789sSS', '123456778899pWW',
        '1123355778899s2s', '1122335778899p5p',
        '445566m5s445566p5s', '[CCC]11123444789p', '[CCC]45666678999p',
        '[456s2][234s3]1223678s2s', '[567m]2333445667m8m', '123456m45679p66s8p'
    )
    foreach ($row in $rows) {
        if ($row.Request.Split('|')[1] -in $regressions -and !$selected.Contains($row)) { $selected.Add($row) }
    }
    foreach ($row in ($rows | Select-Object -Last 7)) {
        if (!$selected.Contains($row)) { $selected.Add($row) }
    }
    '# Frozen from C++ revision 44a178af08bf11f82a8993fddbe2fe8876ddd8f3 (default feature flags).'
    '# Input cases derived from Jeff Wang unit_test.cpp, MIT license; see LICENSE and NOTICE.'
    '# Request<TAB>total;nonzero fan counts. Omitted fan entries must be zero.'
    $seen = [System.Collections.Generic.HashSet[string]]::new()
    $distinct = @($selected | Where-Object { $seen.Add($_.Request) })
    foreach ($row in $distinct) { "$($row.Request)`t$($row.Result)" }
    [Console]::Error.WriteLine("$($distinct.Count) fixtures; $($covered.Count)/$($names.Count) fan entries covered")
    [Console]::Error.WriteLine("Uncovered: $(($names | Where-Object { !$covered.Contains($_) }) -join ', ')")
} finally {
    $process.StandardInput.Close()
    if (!$process.WaitForExit(3000)) { $process.Kill() }
    $process.Dispose()
}
