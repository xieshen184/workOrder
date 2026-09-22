$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $projectRoot 'ruoyi-admin\target\ruoyi-admin.jar'

if (-not (Test-Path -LiteralPath $jarPath)) {
    throw "Backend artifact not found. Run bin\build-local.ps1 first."
}

$env:SPRING_PROFILES_ACTIVE = 'local'
java -jar $jarPath

