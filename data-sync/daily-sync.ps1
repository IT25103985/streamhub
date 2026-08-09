# =====================================================================
# daily-sync.ps1
#
# Run this once a day. It will:
#   1. Switch to dev and pull the latest code + everyone's data files
#   2. Apply any teammates' new/changed rows to YOUR local StreamHubDB,
#      in the correct table order, skipping anything already applied
#   3. Export YOUR new/changed rows since your last sync
#   4. Commit and push that export file
#   5. Switch you back to the branch you were on
#
# One-time setup before first use:
#   - Create data-sync/member.config containing just your first name
#     in lowercase, e.g.  sandaruwan
#   - Make sure sqlcmd is installed (comes with SSMS / SQL Server tools)
#   - Run docs/schema.sql once against your local StreamHubDB
#
# Usage:  powershell -ExecutionPolicy Bypass -File data-sync\daily-sync.ps1
# =====================================================================

param(
    [string]$SqlServerInstance = "localhost\SQLEXPRESS",
    [string]$Database = "StreamHubDB"
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path "$PSScriptRoot\.."
Set-Location $repoRoot

$configFile = "data-sync\member.config"
if (-not (Test-Path $configFile)) {
    Write-Host "Missing data-sync\member.config. Create it with just your first name (lowercase) on one line, e.g. sandaruwan" -ForegroundColor Red
    exit 1
}
$Member = (Get-Content $configFile -Raw).Trim()

$tableOrder = @("User","Category","Video","Playlist","PlaylistVideo","Comment","Like","Report","WatchHistory")

$originalBranch = (git rev-parse --abbrev-ref HEAD).Trim()
Write-Host "Starting daily sync for '$Member' (currently on branch '$originalBranch')" -ForegroundColor Cyan

# --- Step 1: switch to dev and pull latest ---
git checkout dev | Out-Null
git pull origin dev

# --- Step 2: import teammates' new data ---
Add-Type -AssemblyName "System.Data"
$connString = "Server=$SqlServerInstance;Database=$Database;Integrated Security=True;TrustServerCertificate=True;"
$conn = New-Object System.Data.SqlClient.SqlConnection($connString)
$conn.Open()

$cmdCheck = $conn.CreateCommand()
$cmdCheck.CommandText = "SELECT file_name FROM SyncLog"
$reader = $cmdCheck.ExecuteReader()
$alreadyApplied = @()
while ($reader.Read()) { $alreadyApplied += $reader["file_name"] }
$reader.Close()

$syncFiles = Get-ChildItem "data-sync" -Filter "*.sql" -File -ErrorAction SilentlyContinue
$pending = $syncFiles | Where-Object {
    ($alreadyApplied -notcontains $_.Name) -and (-not $_.Name.StartsWith("$Member`_"))
}

if ($pending.Count -gt 0) {
    Write-Host "Applying $($pending.Count) new sync file(s) from teammates..." -ForegroundColor Cyan

    $blocks = @{}
    foreach ($t in $tableOrder) { $blocks[$t] = New-Object System.Collections.Generic.List[string] }

    foreach ($file in $pending) {
        $content = Get-Content $file.FullName -Raw
        $sections = $content -split "-- TABLE:"
        foreach ($section in $sections) {
            if ([string]::IsNullOrWhiteSpace($section)) { continue }
            $parts = $section -split "`r?`n", 2
            $tableName = $parts[0].Trim()
            $body = if ($parts.Count -gt 1) { $parts[1] } else { "" }
            if (($tableOrder -contains $tableName) -and (-not [string]::IsNullOrWhiteSpace($body))) {
                $blocks[$tableName].Add($body)
            }
        }
    }

    foreach ($t in $tableOrder) {
        foreach ($body in $blocks[$t]) {
            $statements = $body -split "`r?`n" | Where-Object { $_.Trim().StartsWith("INSERT") }
            foreach ($stmt in $statements) {
                try {
                    $cmd = $conn.CreateCommand()
                    $cmd.CommandText = $stmt
                    $cmd.ExecuteNonQuery() | Out-Null
                } catch {
                    Write-Host "  Skipped one row in $t (likely already exists): $($_.Exception.Message)" -ForegroundColor DarkYellow
                }
            }
        }
    }

    foreach ($file in $pending) {
        $cmdLog = $conn.CreateCommand()
        $cmdLog.CommandText = "INSERT INTO SyncLog (file_name) VALUES (@f)"
        $p = $cmdLog.CreateParameter()
        $p.ParameterName = "@f"
        $p.Value = $file.Name
        $cmdLog.Parameters.Add($p) | Out-Null
        $cmdLog.ExecuteNonQuery() | Out-Null
    }
    Write-Host "Import complete." -ForegroundColor Green
} else {
    Write-Host "No new teammate data to import." -ForegroundColor Gray
}
$conn.Close()

# --- Step 3: export my own changes since last export ---
$stateDir = "data-sync\.state"
New-Item -ItemType Directory -Path $stateDir -Force | Out-Null
$stateFile = "$stateDir\$Member.txt"
$since = if (Test-Path $stateFile) { (Get-Content $stateFile -Raw).Trim() } else { "2026-08-01T00:00:00" }
$today = Get-Date -Format "yyyy-MM-dd"
$outFile = "data-sync\${Member}_$today.sql"

$exportTemplatePath = "data-sync\scripts\export_changes.sql"
$exportSql = Get-Content $exportTemplatePath -Raw
$exportSql = $exportSql -replace '\$\(Since\)', $since
$tempExportPath = "data-sync\scripts\.tmp_export_changes.sql"
Set-Content -Path $tempExportPath -Value $exportSql -NoNewline

$sqlcmdArgs = @(
    "-S", $SqlServerInstance,
    "-d", $Database,
    "-E",
    "-i", $tempExportPath,
    "-o", $outFile,
    "-W"
)
& sqlcmd @sqlcmdArgs

Remove-Item $tempExportPath -Force -ErrorAction SilentlyContinue

$hasData = @(Get-Content $outFile -ErrorAction SilentlyContinue | Where-Object { $_.Trim().StartsWith("INSERT") }).Count -gt 0

if ($hasData) {
    git add $outFile
    git commit -m "Data sync: $Member $today"
    git push origin dev
    Set-Content -Path $stateFile -Value (Get-Date -Format "o")
    Write-Host "Exported and pushed your changes ($outFile)." -ForegroundColor Green
} else {
    if (Test-Path $outFile) { Remove-Item $outFile -Force }
    Write-Host "No new changes to export today." -ForegroundColor Gray
}

# --- Step 4: return to original branch ---
git checkout $originalBranch | Out-Null
Write-Host "Done. Back on branch '$originalBranch'." -ForegroundColor Cyan
