$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($env:CHSS_DOCKER_IMAGE) -or
    [string]::IsNullOrWhiteSpace($env:DOCKER_CONFIG)) {
    throw 'The pushed image name or temporary Docker config is unavailable.'
}

$targetDirectory = Join-Path $env:WORKSPACE 'community-health-survey\chss\target'
New-Item -ItemType Directory -Path $targetDirectory -Force | Out-Null
$evidencePath = Join-Path $targetDirectory "docker-hub-manifest-build-$env:BUILD_NUMBER.json"

$manifest = & docker --config $env:DOCKER_CONFIG manifest inspect --verbose $env:CHSS_DOCKER_IMAGE 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "Docker Hub manifest lookup failed for $env:CHSS_DOCKER_IMAGE."
}

$manifest | Set-Content -LiteralPath $evidencePath -Encoding UTF8
Write-Host "Docker Hub registry manifest verified for $env:CHSS_DOCKER_IMAGE"
Write-Host "Registry evidence saved to $evidencePath"
$manifest | ForEach-Object { Write-Host $_ }
