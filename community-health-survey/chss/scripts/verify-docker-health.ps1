param(
    [ValidateRange(1, 300)]
    [int] $TimeoutSeconds = 90
)

$ErrorActionPreference = 'Stop'
$uri = 'http://localhost:18082/login'
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$lastConnectionError = 'No response received.'

while ((Get-Date) -lt $deadline) {
    try {
        $response = Invoke-WebRequest -Uri $uri -UseBasicParsing -TimeoutSec 5
    }
    catch {
        $httpResponse = $_.Exception.Response
        if ($httpResponse) {
            $status = [int] $httpResponse.StatusCode
            if ($status -lt 200 -or $status -ge 300) {
                Write-Error "Application health check received non-2xx HTTP status $status."
                exit 1
            }
        }
        $lastConnectionError = $_.Exception.GetType().Name
        Start-Sleep -Seconds 2
        continue
    }

    $status = [int] $response.StatusCode
    if ($status -lt 200 -or $status -ge 300) {
        Write-Error "Application health check received non-2xx HTTP status $status."
        exit 1
    }
    Write-Host "Application health check passed: HTTP $status at $uri"
    exit 0
}

Write-Error "Application health check failed after $TimeoutSeconds seconds; last connection error: $lastConnectionError"
exit 1
