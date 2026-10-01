param(
    [string]$WorkspaceRoot = $env:WORKSPACE
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($WorkspaceRoot)) {
    $WorkspaceRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
}

$pidFile = Join-Path $WorkspaceRoot '.chss-selenium-app.pid'
if (-not (Test-Path -LiteralPath $pidFile)) {
    Write-Output 'No Selenium application PID file found; nothing to stop.'
    exit 0
}

$applicationProcessId = 0
if (-not [int]::TryParse((Get-Content -LiteralPath $pidFile -Raw), [ref]$applicationProcessId)) {
    Remove-Item -LiteralPath $pidFile -Force
    throw 'Selenium application PID file was invalid and has been removed.'
}

if (Get-Process -Id $applicationProcessId -ErrorAction SilentlyContinue) {
    & taskkill.exe /PID $applicationProcessId /T /F | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Could not stop Selenium application process tree rooted at PID $applicationProcessId."
    }
    Write-Output "Stopped Selenium application process tree rooted at PID $applicationProcessId."
}

Remove-Item -LiteralPath $pidFile -Force
