# EarthSMP — spójny backup offline, Windows PowerShell 5.1+
# URUCHAMIAJ TYLKO PO ZATRZYMANIU PAPER: stop i zamknięta konsola serwera.
# Archiwum zawiera swiaty, dane pluginow i lokalne konfiguracje, w tym server.properties.
# PRZECHOWUJ PRYWATNIE: archiwum zawiera haslo RCON i informacje administracyjne.
param([switch]$ConfirmStopped)
$ErrorActionPreference = 'Stop'
$serverDir = $PSScriptRoot
$backupDir = Join-Path (Split-Path -Parent $serverDir) 'EarthSMP_Backups'
$properties = Join-Path $serverDir 'server.properties'
$stage = $null
$temporaryZip = $null
$mutex = New-Object -TypeName System.Threading.Mutex -ArgumentList @($false, 'Local\EarthSMP_Backup')
$mutexHeld = $false

function Copy-Directory([string]$source, [string]$dest, [string[]]$excludeDirs = @(), [string[]]$excludeFiles = @()) {
    if (-not (Test-Path -LiteralPath $source -PathType Container)) { return }
    New-Item -ItemType Directory -Path $dest -Force | Out-Null
    $arguments = @($source, $dest, '/E', '/R:2', '/W:1', '/NFL', '/NDL', '/NJH', '/NJS', '/NP')
    if ($excludeDirs.Count -gt 0) { $arguments += '/XD'; $arguments += $excludeDirs }
    if ($excludeFiles.Count -gt 0) { $arguments += '/XF'; $arguments += $excludeFiles }
    & robocopy.exe @arguments | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "Nie udalo sie skopiowac folderu $source. Robocopy: $LASTEXITCODE" }
}

try {
    $mutexHeld = $mutex.WaitOne(0)
    if (-not $mutexHeld) { throw 'Juz trwa inna kopia zapasowa.' }
    if (-not $ConfirmStopped) { throw 'Najpierw zatrzymaj Paper komenda stop, potem uruchom .\EarthSMP-Backup.ps1 -ConfirmStopped' }
    if (-not (Test-Path -LiteralPath $properties -PathType Leaf)) { throw 'Brakuje server.properties.' }

    $lines = @(Get-Content -LiteralPath $properties)
    $worldLine = $lines | Where-Object { $_ -match '^level-name=' } | Select-Object -Last 1
    $portLine = $lines | Where-Object { $_ -match '^server-port=' } | Select-Object -Last 1
    if (-not $worldLine) { throw 'Brakuje level-name w server.properties.' }
    $worldName = $worldLine.Substring('level-name='.Length).Trim()
    if (-not $worldName -or [IO.Path]::GetFileName($worldName) -ne $worldName) { throw 'Niepoprawny level-name.' }
    $worldDir = Join-Path $serverDir $worldName
    if (-not (Test-Path -LiteralPath (Join-Path $worldDir 'level.dat') -PathType Leaf)) { throw "Nie znaleziono swiata: $worldName" }
    $database = Join-Path $serverDir 'plugins\EarthSMPCore\earthsmp.db'
    if (-not (Test-Path -LiteralPath $database -PathType Leaf)) { throw 'Brakuje plugins/EarthSMPCore/earthsmp.db — backup przerwany.' }

    # Dodatkowa kontrola portu serwera; sama komenda -ConfirmStopped pozostaje wymagana.
    $port = 25565
    if ($portLine) { $port = [int]$portLine.Substring('server-port='.Length).Trim() }
    $listener = @(Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue)
    if ($listener.Count -gt 0) { throw "Port $port jest nadal nasluchiwany. Zatrzymaj serwer i ponow backup." }

    # Windows blokuje jednoczesne otwarcie uzywanego session.lock przez inny proces.
    $session = Join-Path $worldDir 'session.lock'
    if (Test-Path -LiteralPath $session -PathType Leaf) {
        try {
            $stream = [IO.File]::Open($session, [IO.FileMode]::Open, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
            $stream.Dispose()
        } catch { throw 'Plik session.lock jest zablokowany. Paper moze wciaz dzialac.' }
    }

    New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
    $stamp = Get-Date -Format 'yyyy-MM-dd_HH-mm-ss'
    $stage = Join-Path $backupDir ('.staging_' + $stamp + '_' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $stage -Force | Out-Null
    $watch = [Diagnostics.Stopwatch]::StartNew()

    # Glowne i dodatkowe swiaty (Nether/End moga znajdowac sie wewnatrz Earth).
    foreach ($name in @($worldName, 'world', 'world_nether', 'world_the_end') | Select-Object -Unique) {
        Copy-Directory (Join-Path $serverDir $name) (Join-Path $stage $name) -excludeFiles @('session.lock')
    }
    Copy-Directory (Join-Path $serverDir 'config') (Join-Path $stage 'config')

    # Wszystkie DANE pluginow oprocz kosztownych, odtwarzalnych kafelkow squaremap oraz JAR-ow.
    $plugins = Join-Path $serverDir 'plugins'
    Copy-Directory $plugins (Join-Path $stage 'plugins') `
        -excludeDirs @((Join-Path $plugins 'squaremap\web'), (Join-Path $plugins 'squaremap\data')) `
        -excludeFiles @('*.jar')

    # Konfiguracja Paper i lista dostepu; zawiera tez prywatny server.properties.
    foreach ($filename in @('server.properties', 'bukkit.yml', 'spigot.yml', 'commands.yml', 'eula.txt',
                            'whitelist.json', 'ops.json', 'wepif.yml', 'start.bat')) {
        $source = Join-Path $serverDir $filename
        if (Test-Path -LiteralPath $source -PathType Leaf) { Copy-Item -LiteralPath $source -Destination (Join-Path $stage $filename) }
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $temporaryZip = Join-Path $backupDir ('.temporary_' + [guid]::NewGuid().ToString('N') + '.zip')
    [IO.Compression.ZipFile]::CreateFromDirectory($stage, $temporaryZip, [IO.Compression.CompressionLevel]::Optimal, $false)
    $archive = Join-Path $backupDir ('EarthSMP_' + $stamp + '.zip')
    $zip = [IO.Compression.ZipFile]::OpenRead($temporaryZip)
    try {
        $entries = @($zip.Entries | ForEach-Object { $_.FullName.Replace('\', '/') })
        if ($entries -notcontains ($worldName + '/level.dat')) { throw 'Kopia ZIP nie zawiera level.dat.' }
        if ($entries -notcontains 'plugins/EarthSMPCore/earthsmp.db') { throw 'Kopia ZIP nie zawiera bazy państw.' }
        $prefix = $worldName + '/dimensions/minecraft/overworld/region/'
        if (@($entries | Where-Object { $_.StartsWith($prefix) -and $_.EndsWith('.mca') }).Count -eq 0) {
            throw 'Kopia ZIP nie zawiera regionow mapy Earth.'
        }
    } finally { $zip.Dispose() }
    Move-Item -LiteralPath $temporaryZip -Destination $archive -ErrorAction Stop
    $temporaryZip = $null
    $watch.Stop()
    $megabytes = [math]::Round((Get-Item -LiteralPath $archive).Length / 1MB, 1)
    Write-Host "SUKCES: $archive ($megabytes MB, $([math]::Round($watch.Elapsed.TotalSeconds, 1)) s)" -ForegroundColor Green
    Write-Host 'WAZNE: ZIP zawiera server.properties (haslo RCON). Nie publikuj go.' -ForegroundColor Yellow

    # Retencja dopiero po utworzeniu i sprawdzeniu nowej kopii.
    Get-ChildItem -LiteralPath $backupDir -Filter 'EarthSMP_*.zip' -File |
        Sort-Object LastWriteTime -Descending | Select-Object -Skip 12 | Remove-Item -Force
} catch {
    Write-Host "BLAD BACKUPU: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
} finally {
    if ($temporaryZip -and (Test-Path -LiteralPath $temporaryZip)) { Remove-Item -LiteralPath $temporaryZip -Force -ErrorAction SilentlyContinue }
    if ($stage -and (Test-Path -LiteralPath $stage)) { Remove-Item -LiteralPath $stage -Recurse -Force -ErrorAction SilentlyContinue }
    if ($mutexHeld) { $mutex.ReleaseMutex() }
    $mutex.Dispose()
}
