$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($env:DOCKERHUB_USERNAME) -or
    [string]::IsNullOrWhiteSpace($env:DOCKERHUB_TOKEN) -or
    [string]::IsNullOrWhiteSpace($env:DOCKER_CONFIG)) {
    throw 'Docker Hub Jenkins credentials or temporary Docker config are unavailable.'
}

New-Item -ItemType Directory -Path $env:DOCKER_CONFIG -Force | Out-Null

# Read the PAT from the masked Jenkins environment and send it only on stdin.
$env:DOCKERHUB_TOKEN | & docker --config $env:DOCKER_CONFIG login `
    --username $env:DOCKERHUB_USERNAME --password-stdin
if ($LASTEXITCODE -ne 0) {
    throw "Docker Hub login failed with exit code $LASTEXITCODE."
}

Write-Host 'Docker Hub authentication succeeded.'
