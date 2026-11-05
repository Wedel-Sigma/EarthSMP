# Earth SMP — manual backup (Windows PowerShell 5.1+) — fix ZIP check v1.1
# The script must be placed next to paper.jar, mcrcon.exe and server.properties.
# The Minecraft server must be running with RCON enabled on localhost:25575.

$ErrorActionPreference = 'Stop'
$serverDir  = $PSScriptRoot
$backupDir  = Join-Path (Split-Path -Parent $serverDir) 'EarthSMP_Backups'
$propsFile  = Join-Path $serverDir 'server.properties'
$mcrconExe  = Join-Path $serverDir 'mcrcon.exe'
$stageDir   = $null
$timer      = $null
$exitCode   = 0
$mutex      = New-Object -TypeName System.Threading.Mutex -ArgumentList @($false, 'Local\EarthSMP_Backup')
$mutexHeld  = $false

function Invoke-Rcon([string]$command) {
    $output = & $script:mcrconExe -H '127.0.0.1' -P 25575 $command 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "RCON: blad wykonywania '$command': $($output -join ' ')"
    }
    return ($output -join "`n")
}

try {
    $mutexHeld = $mutex.WaitOne(0)
    if (-not $mutexHeld) { throw 'Backup juz trwa w innym procesie.' }
    if (-not (Test-Path -LiteralPath $propsFile -PathType Leaf)) { throw 'Brak server.properties obok skryptu.' }
    if (-not (Test-Path -LiteralPath $mcrconExe -PathType Leaf)) { throw 'Brak mcrcon.exe obok skryptu.' }

    $props = @(Get-Content -LiteralPath $propsFile)
    $worldLine = $props | Where-Object { $_ -match '^level-name=' } | Select-Object -Last 1
    $passLine  = $props | Where-Object { $_ -match '^rcon\.password=' } | Select-Object -Last 1
    if (-not $worldLine) { throw 'Nie znaleziono level-name w server.properties.' }
    if (-not $passLine)  { throw 'Nie znaleziono rcon.password w server.properties.' }

    $worldName = $worldLine.Substring('level-name='.Length).Trim()
    $password  = $passLine.Substring('rcon.password='.Length).Trim()
    if (-not $worldName -or -not $password) { throw 'Pusta nazwa swiata lub haslo RCON.' }
    if ([System.IO.Path]::GetFileName($worldName) -ne $worldName) { throw 'level-name musi byc nazwa folderu swiata.' }
    $worldDir = Join-Path $serverDir $worldName
    if (-not (Test-Path -LiteralPath (Join-Path $worldDir 'level.dat') -PathType Leaf)) {
        throw "Nie znaleziono level.dat w '$worldDir'."
    }
    $env:MCRCON_PASS = $password
    $password = $null

    # Check connectivity before issuing the 10-second warning.
    Invoke-Rcon 'list' | Out-Null
    New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
    Write-Host 'Polaczenie RCON OK. Backup rozpocznie sie za 10 sekund.'
    Invoke-Rcon 'say [Backup] Backup rozpocznie sie za 10 sekund.' | Out-Null
    Start-Sleep -Seconds 10

    $timer = [Diagnostics.Stopwatch]::StartNew()
    Invoke-Rcon 'say [Backup] Backup wystartowal.' | Out-Null
    Write-Host 'Backup w toku...'

    $stamp    = Get-Date -Format 'yyyy-MM-dd_HH-mm-ss'
    $stageDir = Join-Path $backupDir ('.staging_' + $stamp + '_' + [guid]::NewGuid().ToString('N'))
    $stageWorld = Join-Path $stageDir $worldName
    $archive = Join-Path $backupDir ('EarthSMP_' + $stamp + '.zip')
    New-Item -ItemType Directory -Path $stageWorld -Force | Out-Null

    $saveOffAttempted = $false
    try {
        # Forced save-all flush still works when autosaving is disabled.
        $saveOffAttempted = $true
        $offResult = Invoke-Rcon 'save-off'
        if ($offResult -match '(?i)already|failed|error|unknown') { throw "save-off: $offResult" }

        $flushResult = Invoke-Rcon 'save-all flush'
        if ($flushResult -match '(?i)failed|error|unknown') { throw "save-all flush: $flushResult" }

        # Copy quickly while level saves are off; compress AFTER resuming saves.
        & robocopy.exe $worldDir $stageWorld /E /R:2 /W:1 /XF 'session.lock' /NFL /NDL /NJH /NJS /NP | Out-Null
        $copyExit = $LASTEXITCODE
        if ($copyExit -ge 8) { throw "Robocopy zakonczyl sie bledem (kod $copyExit)." }
    }
    finally {
        if ($saveOffAttempted) {
            # Important: attempt to re-enable saving even if copying failed.
            Invoke-Rcon 'save-on' | Out-Null
        }
    }

    if (-not (Test-Path -LiteralPath (Join-Path $stageWorld 'level.dat'))) {
        throw 'Brak level.dat w kopii roboczej.'
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::CreateFromDirectory(
        $stageDir, $archive, [System.IO.Compression.CompressionLevel]::Optimal, $false
    )

    if ((Get-Item -LiteralPath $archive).Length -eq 0) { throw 'Plik ZIP jest pusty.' }
    $zip = [System.IO.Compression.ZipFile]::OpenRead($archive)
    try {
        # On Windows PowerShell 5.1, .NET Framework can use \ inside ZIP entries.
        # Normalize the entry names before checking the archive structure.
        $entryNames = @($zip.Entries | ForEach-Object { $_.FullName.Replace('\', '/') })
        $foundLevel = $entryNames -contains ($worldName + '/level.dat')
        if (-not $foundLevel) {
            $sample = ($entryNames | Select-Object -First 5) -join ', '
            throw "W ZIP nie znaleziono level.dat. Pierwsze wpisy: $sample"
        }
        $regionPrefix = $worldName + '/dimensions/minecraft/overworld/region/'
        $foundRegion = @($entryNames | Where-Object { $_.StartsWith($regionPrefix) -and $_.EndsWith('.mca') }).Count -gt 0
        if (-not $foundRegion) {
            throw 'W ZIP nie znaleziono plikow region/*.mca mapy Earth.'
        }
    }
    finally { $zip.Dispose() }

    $timer.Stop()
    $duration = $timer.Elapsed.TotalSeconds.ToString('0.000', [Globalization.CultureInfo]::InvariantCulture)
    $sizeMB = [math]::Round((Get-Item -LiteralPath $archive).Length / 1MB, 1)
    Write-Host "SUKCES: $archive | $sizeMB MB | $duration s"
    Invoke-Rcon "say [Backup] Backup zakonczony pomyslnie! Czas: $duration s." | Out-Null

    # Keep only the 12 most recent completed archives.
    Get-ChildItem -LiteralPath $backupDir -Filter 'EarthSMP_*.zip' -File |
        Sort-Object LastWriteTime -Descending |
        Select-Object -Skip 12 |
        Remove-Item -Force
}
catch {
    $exitCode = 1
    Write-Host "BLAD BACKUPU: $($_.Exception.Message)" -ForegroundColor Red
    if ($timer) { $timer.Stop() }
    try { if ($env:MCRCON_PASS) { Invoke-Rcon 'say [Backup] BLAD! Kopia zapasowa nie zostala zakonczona.' | Out-Null } } catch {}
    Write-Host 'Jesli w konsoli pojawi sie blad save-on, wpisz recznie: save-on' -ForegroundColor Yellow
}
finally {
    if ($stageDir -and (Test-Path -LiteralPath $stageDir)) {
        Remove-Item -LiteralPath $stageDir -Recurse -Force -ErrorAction SilentlyContinue
    }
    Remove-Item Env:\MCRCON_PASS -ErrorAction SilentlyContinue
    if ($mutexHeld) { $mutex.ReleaseMutex() }
    $mutex.Dispose()
}
exit $exitCode
