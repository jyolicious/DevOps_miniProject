param(
    [Parameter(Mandatory = $true)]
    [string] $Image
)

$ErrorActionPreference = 'Stop'
$containerName = 'chss-container'

function Fail-WithDiagnostics([string] $Message) {
    Write-Host $Message
    Write-Host 'Container list:'
    & docker ps -a --filter 'name=^/chss-container$' --format 'ID={{.ID}} IMAGE={{.Image}} STATUS={{.Status}} PORTS={{.Ports}} NAMES={{.Names}}'
    if ($LASTEXITCODE -eq 0) {
        $lines = & docker logs --tail 100 $containerName 2>&1
        if ($LASTEXITCODE -eq 0) {
            $lines | Select-String -Pattern 'Starting CommunityHealthSurveyApplication|Tomcat initialized|Tomcat started|Started CommunityHealthSurveyApplication|ERROR|Exception' | ForEach-Object { Write-Host $_.Line }
        }
    }
    exit 1
}

$deadline = (Get-Date).AddSeconds(90)
$metadata = $null
$parts = $null
$startupConfirmed = $false
do {
    $metadata = & docker inspect --format '{{.Id}}|{{.Config.Image}}|{{.State.Running}}|{{json .NetworkSettings.Ports}}|{{json .Config.Labels}}' $containerName 2>$null
    if ($LASTEXITCODE -eq 0 -and $metadata) {
        $parts = $metadata -split '\|', 5
        $labels = $parts[4] | ConvertFrom-Json
        $appLabel = $labels.PSObject.Properties['com.chss.application'].Value
        if ($parts.Count -ne 5 -or $parts[1] -ne $Image -or
            $parts[3] -notmatch '"HostPort":"18082"' -or $appLabel -ne 'community-health-survey-system') {
            Fail-WithDiagnostics 'Container image, managed label or 18082:8081 port mapping did not match the deployment.'
        }
        if ($parts[2] -ne 'true') {
            Fail-WithDiagnostics 'The newly deployed CHSS container exited before application verification.'
        }

        $logLines = & docker logs --tail 100 $containerName 2>&1
        if ($LASTEXITCODE -ne 0) {
            Fail-WithDiagnostics 'docker logs could not read the new container.'
        }
        if ($logLines -match 'Started CommunityHealthSurveyApplication|Tomcat started on port 8081') {
            $startupConfirmed = $true
            break
        }
    }
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $deadline)

if (-not $startupConfirmed -or -not $parts) {
    Fail-WithDiagnostics 'The application did not reach a running, started state within 90 seconds.'
}

Write-Host 'Running Docker containers:'
& docker ps --filter "name=^/$containerName$" --format 'ID={{.ID}} IMAGE={{.Image}} STATUS={{.Status}} PORTS={{.Ports}} NAMES={{.Names}}'
Write-Host 'All CHSS deployment containers (including stopped):'
& docker ps -a --filter "name=^/$containerName$" --format 'ID={{.ID}} IMAGE={{.Image}} STATUS={{.Status}} PORTS={{.Ports}} NAMES={{.Names}}'
Write-Host ("Container ID: " + $parts[0])
Write-Host ("Image: " + $parts[1])
Write-Host ("Running: " + $parts[2])
Write-Host ("Port bindings: " + $parts[3])
Write-Host 'Application startup log milestones:'
$milestones = $logLines | Select-String -Pattern 'Starting CommunityHealthSurveyApplication|Tomcat initialized|Tomcat started|Started CommunityHealthSurveyApplication'
$milestones | ForEach-Object { Write-Host $_.Line }
if (-not ($milestones | Where-Object { $_.Line -match 'Started CommunityHealthSurveyApplication|Tomcat started on port 8081' })) {
    Fail-WithDiagnostics 'Application startup was not confirmed in container logs.'
}

Write-Host 'Container verification passed.'
