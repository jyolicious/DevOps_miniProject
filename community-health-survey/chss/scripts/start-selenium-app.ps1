param(
    [int]$Port = 8181,
    [int]$TimeoutSeconds = 120,
    [string]$WorkspaceRoot = $env:WORKSPACE
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($WorkspaceRoot)) {
    $WorkspaceRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
}

$pidFile = Join-Path $WorkspaceRoot '.chss-selenium-app.pid'
$logFile = Join-Path $WorkspaceRoot 'selenium-test-app.log'
$errorLogFile = Join-Path $WorkspaceRoot 'selenium-test-app-error.log'
$client = [System.Net.Sockets.TcpClient]::new()
try {
    $client.Connect('127.0.0.1', $Port)
    if ($client.Connected) {
        throw "Port $Port is already in use. The test runner will not attach to or stop another application."
    }
} catch [System.Net.Sockets.SocketException] {
    # No process is listening on the selected test port.
} finally {
    $client.Dispose()
}

if (Test-Path -LiteralPath $pidFile) {
    Remove-Item -LiteralPath $pidFile -Force
}

$pom = Join-Path $WorkspaceRoot 'community-health-survey\chss\pom.xml'
$mavenUserHome = if ($env:MAVEN_USER_HOME) { $env:MAVEN_USER_HOME } else { Join-Path $env:USERPROFILE '.m2' }
$mavenCache = Join-Path $mavenUserHome 'wrapper\dists'
$mavenCommand = Get-ChildItem -LiteralPath $mavenCache -Filter 'mvn.cmd' -File -Recurse -ErrorAction SilentlyContinue |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $mavenCommand) {
    throw 'Maven Wrapper distribution is not cached for the Jenkins account. Run the Build stage first so mvnw.cmd can bootstrap Maven.'
}

# Maven in this Windows environment must use the account's real profile cache.
$repository = Join-Path $mavenUserHome 'repository'
$localRepositoryOption = '-Dmaven.repo.local="{0}"' -f $repository
$env:MAVEN_OPTS = ('{0} {1}' -f $env:MAVEN_OPTS, $localRepositoryOption).Trim()

$command = '"{0}" -f "{1}" spring-boot:run -Dspring-boot.run.profiles=selenium -Dspring-boot.run.arguments=--server.port={2}' -f $mavenCommand, $pom, $Port
$cmdArguments = '/d /s /c "' + $command + '"'
$applicationProcess = Start-Process -FilePath $env:ComSpec -ArgumentList $cmdArguments `
    -WorkingDirectory $WorkspaceRoot -PassThru -WindowStyle Hidden `
    -RedirectStandardOutput $logFile -RedirectStandardError $errorLogFile
Set-Content -LiteralPath $pidFile -Value $applicationProcess.Id -NoNewline

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
while ((Get-Date) -lt $deadline) {
    $runningProcess = Get-Process -Id $applicationProcess.Id -ErrorAction SilentlyContinue
    if (-not $runningProcess) {
        $tail = @(
            if (Test-Path -LiteralPath $logFile) { Get-Content -LiteralPath $logFile -Tail 20 }
            if (Test-Path -LiteralPath $errorLogFile) { Get-Content -LiteralPath $errorLogFile -Tail 10 }
        ) -join [Environment]::NewLine
        throw "The Selenium application exited during startup. Log tail:`n$tail"
    }

    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:$Port/login" -TimeoutSec 3
        if ($response.StatusCode -eq 200) {
            Write-Output "Selenium application is ready at http://localhost:$Port/login (PID $($applicationProcess.Id))."
            exit 0
        }
    } catch {
        # Keep polling until the application is ready or the timeout is reached.
    }
    Start-Sleep -Seconds 2
}

$tail = @(
    if (Test-Path -LiteralPath $logFile) { Get-Content -LiteralPath $logFile -Tail 20 }
    if (Test-Path -LiteralPath $errorLogFile) { Get-Content -LiteralPath $errorLogFile -Tail 10 }
) -join [Environment]::NewLine
throw "The Selenium application did not become ready within $TimeoutSeconds seconds. Log tail:`n$tail"
