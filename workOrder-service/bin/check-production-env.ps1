[CmdletBinding()]
param(
    # Read the same env file used by Docker Compose without printing secret values.
    [string]$EnvFile
)

$ErrorActionPreference = 'Stop'
$issues = [System.Collections.Generic.List[string]]::new()

if (-not [string]::IsNullOrWhiteSpace($EnvFile)) {
    $resolvedEnvFile = (Resolve-Path -LiteralPath $EnvFile).Path
    foreach ($line in Get-Content -LiteralPath $resolvedEnvFile -Encoding UTF8) {
        $candidate = $line.Trim()
        if ($candidate.Length -eq 0 -or $candidate.StartsWith('#')) {
            continue
        }

        $separator = $candidate.IndexOf('=')
        if ($separator -le 0) {
            $issues.Add("Environment file contains an invalid line")
            continue
        }

        $name = $candidate.Substring(0, $separator).Trim()
        $value = $candidate.Substring($separator + 1).Trim()
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    }
}

function Get-RequiredEnvironmentValue {
    param([Parameter(Mandatory = $true)][string]$Name)

    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        $script:issues.Add("$Name is required")
        return $null
    }
    if ($value -match '^<[^>]+>$') {
        $script:issues.Add("$Name still contains a template placeholder")
        return $null
    }
    return $value
}

# Only report variable names and remediation hints. Never print credential values.
$databaseUrl = Get-RequiredEnvironmentValue -Name 'DB_URL'
$databaseUsername = Get-RequiredEnvironmentValue -Name 'DB_USERNAME'
$databasePassword = Get-RequiredEnvironmentValue -Name 'DB_PASSWORD'
$redisHost = Get-RequiredEnvironmentValue -Name 'REDIS_HOST'
$redisPassword = Get-RequiredEnvironmentValue -Name 'REDIS_PASSWORD'
$tokenSecret = Get-RequiredEnvironmentValue -Name 'TOKEN_SECRET'
$uploadPath = Get-RequiredEnvironmentValue -Name 'WORKORDER_UPLOAD_PATH'
$corsOrigins = Get-RequiredEnvironmentValue -Name 'CORS_ALLOWED_ORIGINS'

$profile = [Environment]::GetEnvironmentVariable('SPRING_PROFILES_ACTIVE')
if ($profile -and $profile -ne 'prod') {
    $issues.Add('SPRING_PROFILES_ACTIVE must be prod')
}
if ($databaseUsername -and $databaseUsername.Trim().ToLowerInvariant() -eq 'root') {
    $issues.Add('DB_USERNAME must use a dedicated application account instead of root')
}
if ($tokenSecret) {
    $normalizedSecret = $tokenSecret.ToLowerInvariant()
    $unsafeMarkers = @('change-me', 'changeme', 'replace-me', 'replace_me', 'development-only', 'example', 'default')
    if ($tokenSecret.Length -lt 32) {
        $issues.Add('TOKEN_SECRET must contain at least 32 characters')
    }
    elseif ($unsafeMarkers | Where-Object { $normalizedSecret.Contains($_) }) {
        $issues.Add('TOKEN_SECRET must not use an example or default value')
    }
}
if ($uploadPath -and $uploadPath -notmatch '^(?:[A-Za-z]:[\\/]|/)') {
    $issues.Add('WORKORDER_UPLOAD_PATH must be an absolute path')
}
if ($corsOrigins) {
    foreach ($origin in $corsOrigins.Split(',')) {
        $candidate = $origin.Trim().ToLowerInvariant()
        if ($candidate.Contains('*') -or -not $candidate.StartsWith('https://')) {
            $issues.Add('CORS_ALLOWED_ORIGINS must contain explicit HTTPS origins without wildcards')
            break
        }
    }
}

if ($issues.Count -gt 0) {
    Write-Host 'Production environment validation failed:' -ForegroundColor Red
    foreach ($issue in $issues) {
        Write-Host " - $issue" -ForegroundColor Red
    }
    exit 1
}

Write-Host 'Production environment validation passed.' -ForegroundColor Green
exit 0
