$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

Push-Location $projectRoot
try {
    mvn.cmd clean package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE"
    }
    Write-Host "Backend artifact: $projectRoot\ruoyi-admin\target\ruoyi-admin.jar"
}
finally {
    Pop-Location
}
