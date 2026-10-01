param(
    [ValidateSet("Auto", "Docker", "Native")]
    [string]$InfrastructureMode = "Auto",
    [string]$HBuilderXPath = "",
    [string]$WechatDevToolsPath = ""
)

$ErrorActionPreference = "Stop"

# This script is intentionally read-only. It does not start services, import SQL,
# create environment files, change ports, or expose credential values.
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$serviceRoot = Split-Path -Parent $scriptRoot
$workspaceRoot = Split-Path -Parent $serviceRoot
$pcRoot = Join-Path $workspaceRoot "workOrder-ui"
$mobileRoot = Join-Path $workspaceRoot "workOrder-app-ui"
$results = New-Object System.Collections.Generic.List[object]

function Add-Result
{
    param(
        [string]$Item,
        [ValidateSet("PASS", "WARN", "FAIL")]
        [string]$Status,
        [string]$Detail
    )

    $results.Add([pscustomobject]@{
        Item = $Item
        Status = $Status
        Detail = $Detail
    })
}

function Resolve-Executable
{
    param(
        [string]$CommandName,
        [string[]]$Candidates = @()
    )

    $command = Get-Command $CommandName -ErrorAction SilentlyContinue
    if ($null -ne $command)
    {
        return $command.Source
    }

    foreach ($candidate in $Candidates)
    {
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and
            (Test-Path -LiteralPath $candidate -PathType Leaf))
        {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }
    return $null
}

function Resolve-DesktopShortcutTarget
{
    param([ValidateSet("HBuilderX", "WeChatDevTools")] [string]$Application)

    $desktopPath = [Environment]::GetFolderPath("Desktop")
    if (-not (Test-Path -LiteralPath $desktopPath -PathType Container))
    {
        return $null
    }

    try
    {
        $shell = New-Object -ComObject WScript.Shell
        foreach ($link in Get-ChildItem -LiteralPath $desktopPath -Filter "*.lnk" -File -ErrorAction SilentlyContinue)
        {
            $targetPath = $shell.CreateShortcut($link.FullName).TargetPath
            if (-not (Test-Path -LiteralPath $targetPath -PathType Leaf))
            {
                continue
            }

            $targetFile = Get-Item -LiteralPath $targetPath
            if ($Application -eq "HBuilderX" -and $targetFile.Name -ieq "HBuilderX.exe")
            {
                return $targetFile.FullName
            }
            if ($Application -eq "WeChatDevTools" -and
                $targetFile.VersionInfo.CompanyName -eq "Tencent" -and
                $targetFile.VersionInfo.InternalName -eq "electron.exe")
            {
                return $targetFile.FullName
            }
        }
    }
    catch
    {
        return $null
    }
    return $null
}

function Test-PortListening
{
    param([int]$Port)

    $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    return $null -ne $listener
}

function Test-RequiredFile
{
    param([string]$Item, [string]$Path)

    if (Test-Path -LiteralPath $Path -PathType Leaf)
    {
        Add-Result -Item $Item -Status "PASS" -Detail $Path
    }
    else
    {
        Add-Result -Item $Item -Status "FAIL" -Detail "Missing file: $Path"
    }
}

$javaPath = Resolve-Executable -CommandName "java.exe"
$mavenPath = Resolve-Executable -CommandName "mvn.cmd"
$nodePath = Resolve-Executable -CommandName "node.exe"
$npmPath = Resolve-Executable -CommandName "npm.cmd"

if ($javaPath) { Add-Result "Java" "PASS" $javaPath } else { Add-Result "Java" "FAIL" "java.exe was not found" }
if ($mavenPath) { Add-Result "Maven" "PASS" $mavenPath } else { Add-Result "Maven" "FAIL" "mvn.cmd was not found" }
if ($nodePath) { Add-Result "Node.js" "PASS" $nodePath } else { Add-Result "Node.js" "FAIL" "node.exe was not found" }
if ($npmPath) { Add-Result "npm" "PASS" $npmPath } else { Add-Result "npm" "FAIL" "npm.cmd was not found" }

$fullSqlPath = Join-Path $serviceRoot "sql\00_workorder_full_init.sql"
$seedSqlPath = Join-Path $serviceRoot "sql\14_workorder_integration_seed.sql"
Test-RequiredFile "Complete SQL bundle" $fullSqlPath
Test-RequiredFile "Integration account seed" $seedSqlPath

if (Test-Path -LiteralPath $fullSqlPath -PathType Leaf)
{
    $strictUtf8 = New-Object System.Text.UTF8Encoding($false, $true)
    try
    {
        $sqlContent = [System.IO.File]::ReadAllText($fullSqlPath, $strictUtf8)
        $sourceCount = ([regex]::Matches($sqlContent, "(?m)^-- BEGIN SOURCE:")).Count
        if ($sourceCount -eq 14 -and
            $sqlContent.Contains("CREATE DATABASE IF NOT EXISTS ``workorder``") -and
            $sqlContent.Contains("USE ``workorder``;") -and
            $sqlContent.Contains("'wo_reporter'") -and
            $sqlContent.Contains("'wo_engineer'") -and
            $sqlContent.Contains("'wo_dispatcher'"))
        {
            Add-Result "SQL bundle contract" "PASS" "UTF-8, 14 ordered sources, workorder database, and three integration users"
        }
        else
        {
            Add-Result "SQL bundle contract" "FAIL" "The generated SQL bundle is incomplete or stale"
        }
    }
    catch
    {
        Add-Result "SQL bundle contract" "FAIL" "The SQL bundle is not valid strict UTF-8: $($_.Exception.Message)"
    }
}

if (Test-Path -LiteralPath (Join-Path $pcRoot "node_modules") -PathType Container)
{
    Add-Result "PC dependencies" "PASS" (Join-Path $pcRoot "node_modules")
}
else
{
    Add-Result "PC dependencies" "FAIL" "Run npm.cmd install in $pcRoot"
}

if (Test-Path -LiteralPath (Join-Path $mobileRoot "node_modules") -PathType Container)
{
    Add-Result "Mobile dependencies" "PASS" (Join-Path $mobileRoot "node_modules")
}
else
{
    Add-Result "Mobile dependencies" "FAIL" "Install the mobile project dependencies before compiling"
}

$pcDevelopmentEnv = Join-Path $pcRoot ".env.development"
if (Test-Path -LiteralPath $pcDevelopmentEnv -PathType Leaf)
{
    $pcEnv = [System.IO.File]::ReadAllText($pcDevelopmentEnv)
    if ($pcEnv.Contains("DEV_PROXY_TARGET = 'http://localhost:8080'") -or
        $pcEnv.Contains("DEV_PROXY_TARGET='http://localhost:8080'"))
    {
        Add-Result "PC API proxy" "PASS" "Development requests proxy to http://localhost:8080"
    }
    else
    {
        Add-Result "PC API proxy" "WARN" "Confirm DEV_PROXY_TARGET points to the local backend"
    }
}
else
{
    Add-Result "PC API proxy" "FAIL" "Missing $pcDevelopmentEnv"
}

$manifestPath = Join-Path $mobileRoot "manifest.json"
if (Test-Path -LiteralPath $manifestPath -PathType Leaf)
{
    try
    {
        $manifest = Get-Content -LiteralPath $manifestPath -Raw -Encoding UTF8 | ConvertFrom-Json
        $wechatAppId = $manifest.'mp-weixin'.appid
        if ([string]::IsNullOrWhiteSpace($wechatAppId))
        {
            Add-Result "WeChat AppID" "FAIL" "mp-weixin.appid is empty"
        }
        else
        {
            Add-Result "WeChat AppID" "PASS" "Configured in manifest.json; ownership must be confirmed before upload"
        }
    }
    catch
    {
        Add-Result "WeChat AppID" "FAIL" "manifest.json cannot be parsed: $($_.Exception.Message)"
    }
}
else
{
    Add-Result "WeChat AppID" "FAIL" "Missing $manifestPath"
}

$dockerPath = Resolve-Executable -CommandName "docker.exe" -Candidates @(
    "C:\Program Files\Docker\Docker\resources\bin\docker.exe"
)
$mysqlPath = Resolve-Executable -CommandName "mysql.exe" -Candidates @(
    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
)
$redisCliPath = Resolve-Executable -CommandName "redis-cli.exe" -Candidates @(
    "C:\Program Files\Redis\redis-cli.exe",
    "D:\redis\redis-cli.exe"
)
$mysqlListening = Test-PortListening -Port 3306
$redisListening = Test-PortListening -Port 6379
$nativeReady = (($mysqlPath -or $mysqlListening) -and ($redisCliPath -or $redisListening))

if ($InfrastructureMode -eq "Docker")
{
    if ($dockerPath)
    {
        Add-Result "Infrastructure" "PASS" "Docker mode: $dockerPath"
    }
    else
    {
        Add-Result "Infrastructure" "FAIL" "Docker mode selected, but docker.exe was not found"
    }
}
elseif ($InfrastructureMode -eq "Native")
{
    if ($nativeReady)
    {
        Add-Result "Infrastructure" "PASS" "Native MySQL and Redis commands or listeners are available"
    }
    else
    {
        Add-Result "Infrastructure" "FAIL" "Native mode requires MySQL 8 and Redis commands or running listeners"
    }
}
else
{
    if ($dockerPath)
    {
        Add-Result "Infrastructure" "PASS" "Auto selected Docker: $dockerPath"
    }
    elseif ($nativeReady)
    {
        Add-Result "Infrastructure" "PASS" "Auto selected native MySQL and Redis"
    }
    else
    {
        Add-Result "Infrastructure" "FAIL" "Install/configure Docker, or provide native MySQL 8 and Redis"
    }
}

$composeEnvPath = Join-Path $serviceRoot ".env"
if ($dockerPath -and ($InfrastructureMode -ne "Native"))
{
    if (Test-Path -LiteralPath $composeEnvPath -PathType Leaf)
    {
        Add-Result "Compose environment" "PASS" "$composeEnvPath exists; values were not printed"
    }
    else
    {
        Add-Result "Compose environment" "FAIL" "Create $composeEnvPath from .env.example and replace placeholders"
    }
}
else
{
    Add-Result "Compose environment" "WARN" "Docker was not selected or detected; no credential values were inspected"
}

if (Test-PortListening -Port 8080)
{
    Add-Result "Backend port 8080" "WARN" "Already listening; confirm it is the intended work order backend"
}
else
{
    Add-Result "Backend port 8080" "PASS" "Free for the local backend"
}

$hbuilderCandidates = @(
    "C:\Program Files\HBuilderX\HBuilderX.exe",
    "D:\HBuilderX\HBuilderX.exe",
    "D:\software\HBuilderX\HBuilderX.exe",
    (Join-Path $env:LOCALAPPDATA "Programs\HBuilderX\HBuilderX.exe")
)
$wechatCandidates = @(
    "C:\Program Files\Tencent\WeChatDevTools\wechatdevtools.exe",
    "C:\Program Files (x86)\Tencent\WeChatDevTools\wechatdevtools.exe",
    (Join-Path $env:LOCALAPPDATA "Programs\WeChatDevTools\wechatdevtools.exe")
)
$resolvedHBuilderX = Resolve-Executable -CommandName "HBuilderX.exe" -Candidates (@($HBuilderXPath) + $hbuilderCandidates)
$resolvedWechatTools = Resolve-Executable -CommandName "wechatdevtools.exe" -Candidates (@($WechatDevToolsPath) + $wechatCandidates)
if (-not $resolvedHBuilderX)
{
    $resolvedHBuilderX = Resolve-DesktopShortcutTarget -Application "HBuilderX"
}
if (-not $resolvedWechatTools)
{
    $resolvedWechatTools = Resolve-DesktopShortcutTarget -Application "WeChatDevTools"
}

if ($resolvedHBuilderX)
{
    Add-Result "HBuilderX" "PASS" $resolvedHBuilderX
}
else
{
    Add-Result "HBuilderX" "FAIL" "Path not detected; pass -HBuilderXPath with the installed executable"
}

if ($resolvedWechatTools)
{
    Add-Result "WeChat DevTools" "PASS" $resolvedWechatTools
}
else
{
    Add-Result "WeChat DevTools" "FAIL" "Path not detected; pass -WechatDevToolsPath with the installed executable"
}

$gitCommand = Get-Command "git.exe" -ErrorAction SilentlyContinue
if ($gitCommand)
{
    # Reuse the repository-local exclude file so an unreadable machine-level
    # ignore file cannot abort an otherwise read-only preflight.
    $repositoryExclude = Join-Path $workspaceRoot ".git\info\exclude"
    $gitChanges = @(& $gitCommand.Source -c "core.excludesFile=$repositoryExclude" -C $workspaceRoot status --short 2>$null)
    if ($gitChanges.Count -gt 0)
    {
        Add-Result "Workspace changes" "WARN" "$($gitChanges.Count) changed/untracked paths; preserve them during integration"
    }
    else
    {
        Add-Result "Workspace changes" "PASS" "Working tree is clean"
    }
}

Write-Host ""
Write-Host "Work order integration preflight"
Write-Host "Workspace: $workspaceRoot"
Write-Host "Infrastructure mode: $InfrastructureMode"
Write-Host ""
$results | Format-Table -AutoSize

$passCount = @($results | Where-Object Status -eq "PASS").Count
$warnCount = @($results | Where-Object Status -eq "WARN").Count
$failCount = @($results | Where-Object Status -eq "FAIL").Count
Write-Host "Summary: PASS=$passCount WARN=$warnCount FAIL=$failCount"

if ($failCount -gt 0)
{
    exit 1
}
exit 0
