param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z0-9][a-z0-9._-]*/chss-app:build-[0-9]+$')]
    [string] $Image
)

$ErrorActionPreference = 'Stop'
$containerName = 'chss-container'
$appLabel = 'community-health-survey-system'
$publishedPort = '18082'
$containerPort = '8081'

function Invoke-Docker([string[]] $DockerArguments) {
    & docker @DockerArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker command '$($DockerArguments[0])' failed with exit code $LASTEXITCODE."
    }
}

function Write-SafeDiagnostics {
    Write-Host 'CHSS deployment diagnostics (selected metadata only):'
    & docker ps -a --filter "name=^/$containerName$" --format 'ID={{.ID}} IMAGE={{.Image}} STATUS={{.Status}} PORTS={{.Ports}} NAMES={{.Names}}'
    if ($LASTEXITCODE -eq 0) {
        $logLines = & docker logs --tail 100 $containerName 2>&1
        if ($LASTEXITCODE -eq 0) {
            $logLines | Select-String -Pattern 'Starting CommunityHealthSurveyApplication|Tomcat initialized|Tomcat started|Started CommunityHealthSurveyApplication|ERROR|Exception' | ForEach-Object { Write-Host $_.Line }
        }
    }
}

try {
    if ($env:DOCKER_CONFIG -and -not (Test-Path -LiteralPath $env:DOCKER_CONFIG)) {
        throw 'Temporary Docker Hub authentication config is missing.'
    }

    Write-Host "Pulling the exact pushed image: $Image"
    Invoke-Docker @('--config', $env:DOCKER_CONFIG, 'pull', $Image)

    $existingIds = & docker container ls --all --quiet --filter "name=^/$containerName$"
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not safely inspect the existing CHSS container name.'
    }

    $existingId = $null
    if ($existingIds) {
        $existingId = ($existingIds | Select-Object -First 1).Trim()
        $existing = & docker inspect --format '{{.Config.Image}}|{{json .Config.Labels}}|{{json .HostConfig.PortBindings}}|{{.State.Running}}' $existingId
        if ($LASTEXITCODE -ne 0) {
            throw 'Could not inspect the existing named container; leaving it untouched.'
        }
        $parts = $existing -split '\|', 4
        $isChssImage = $parts[0] -match '(^|/)chss-app:'
        $isChssLabel = $false
        if ($parts.Count -ge 2 -and $parts[1]) {
            $labels = $parts[1] | ConvertFrom-Json
            $isChssLabel = $labels.PSObject.Properties['com.chss.application'].Value -eq $appLabel
        }
        if (-not ($isChssImage -or $isChssLabel)) {
            throw 'The name chss-container belongs to an unrecognized image; refusing to stop or remove it.'
        }

    }

    $competingIds = & docker container ls --quiet --filter "publish=$publishedPort"
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not verify that the requested host port is available.'
    }
    if ($competingIds -and @($competingIds | Where-Object { $_.Trim() -ne $existingId }).Count -gt 0) {
        throw "Host port $publishedPort is used by another running container; refusing deployment."
    }

    $existingOwnsPort = $existingId -and $parts.Count -ge 4 -and
        $parts[2] -match '"HostPort":"18082"' -and $parts[3] -eq 'true'
    $hostListeners = Get-NetTCPConnection -LocalPort ([int]$publishedPort) -State Listen -ErrorAction SilentlyContinue
    if ($hostListeners -and -not $existingOwnsPort) {
        throw "Host port $publishedPort is already in use outside the current CHSS container; refusing deployment."
    }

    if ($existingId) {
        Write-Host 'Replacing the existing CHSS deployment container after image pull and port checks succeeded.'
        Invoke-Docker @('stop', $containerName)
        Invoke-Docker @('rm', $containerName)
    }

    $buildMatch = [regex]::Match($Image, ':build-([0-9]+)$')
    if (-not $buildMatch.Success) {
        throw 'The deployment image does not contain a numeric Jenkins build tag.'
    }
    $databaseName = 'week12_build' + $buildMatch.Groups[1].Value
    $databaseUrl = "jdbc:h2:mem:$databaseName;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"

    Write-Host "Starting fresh container $containerName from $Image on ${publishedPort}:${containerPort}."
    Invoke-Docker @(
        'run', '--detach', '--name', $containerName,
        '--label', "com.chss.application=$appLabel",
        '--label', 'com.chss.managed-by=jenkins-week12',
        '--publish', "${publishedPort}:${containerPort}",
        '--env', 'APP_PORT=8081',
        '--env', "DB_URL=$databaseUrl",
        '--env', 'DB_USERNAME=sa',
        '--env', 'DB_PASSWORD=',
        $Image
    )
    if ($LASTEXITCODE -ne 0) {
        throw 'The fresh CHSS container did not start.'
    }
    Write-Host 'Fresh CHSS container creation command succeeded.'
}
catch {
    Write-Host $_.Exception.Message
    Write-SafeDiagnostics
    exit 1
}
