# 快照脚本：高危修改前把项目目录打包归档，并记录 git HEAD。
# 用法：
#   .\scripts\snapshot.ps1
#   .\scripts\snapshot.ps1 -Restore F:\deepseek-harness\_archive\20261112-0100-cooktimer -Force
[CmdletBinding(DefaultParameterSetName = 'Snapshot')]
param(
    [Parameter(ParameterSetName = 'Restore', Mandatory = $true, Position = 0)]
    [string]$Restore,

    [Parameter(ParameterSetName = 'Restore')]
    [switch]$Force,

    [Parameter(ParameterSetName = 'Snapshot')]
    [string]$ArchiveRoot = 'F:\deepseek-harness\_archive'
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$excludeDirs = @('build', '.gradle', '.venv')
$newline = [Environment]::NewLine

function Test-ExcludedPath {
    param([string]$RelativePath)
    $parts = $RelativePath -split '[\\/]'
    for ($i = 0; $i -lt $parts.Count - 1; $i++) {
        if ($excludeDirs -contains $parts[$i]) { return $true }
    }
    return $false
}

function Get-GitHeadText {
    param([string]$Path)
    Push-Location -LiteralPath $Path
    try {
        $head = & git rev-parse HEAD 2>$null
        if ($LASTEXITCODE -ne 0 -or -not $head) {
            return 'git: 当前目录不是 git 仓库，未记录 HEAD'
        }
        $branch = & git rev-parse --abbrev-ref HEAD 2>$null
        $status = @(& git status --porcelain 2>$null)
        $state = if ($status.Count -eq 0) { '干净' } else { '有未提交改动' }
        $lines = @("HEAD: $head", "分支: $branch", "工作区: $state")
        if ($status.Count -gt 0) { $lines += $status }
        return ($lines -join $newline)
    }
    catch {
        return "git: 读取 HEAD 失败（$($_.Exception.Message)）"
    }
    finally {
        Pop-Location
    }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem

if ($PSCmdlet.ParameterSetName -eq 'Snapshot') {
    $stamp = Get-Date -Format 'yyyyMMdd-HHmm'
    $snapshotDir = Join-Path $ArchiveRoot "$stamp-cooktimer"
    $suffix = 2
    while (Test-Path -LiteralPath $snapshotDir) {
        $snapshotDir = Join-Path $ArchiveRoot "$stamp-cooktimer-$suffix"
        $suffix++
    }
    New-Item -ItemType Directory -Path $snapshotDir -Force | Out-Null
    $zipPath = Join-Path $snapshotDir "StoveSide-$stamp.zip"

    $rootPrefix = $projectRoot.TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar
    $included = New-Object System.Collections.ArrayList
    foreach ($file in Get-ChildItem -LiteralPath $projectRoot -Recurse -File -Force -ErrorAction SilentlyContinue) {
        $relative = $file.FullName.Substring($rootPrefix.Length)
        if (Test-ExcludedPath -RelativePath $relative) { continue }
        [void]$included.Add([pscustomobject]@{
            Source = $file.FullName
            Entry  = $relative.Replace('\', '/')
        })
    }

    Write-Host "项目目录: $projectRoot"
    Write-Host "快照目录: $snapshotDir"
    Write-Host "待归档文件: $($included.Count)（已排除 build/、.gradle/、.venv/）"

    $archive = [System.IO.Compression.ZipFile]::Open($zipPath, 'Create')
    try {
        foreach ($item in $included) {
            [void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $item.Source, $item.Entry, 'Optimal')
        }
    }
    finally {
        $archive.Dispose()
    }

    $headInfo = Get-GitHeadText -Path $projectRoot
    $meta = @(
        'StoveSide 项目快照'
        "时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        "项目: $projectRoot"
        "压缩包: $(Split-Path -Leaf $zipPath)"
        "文件数: $($included.Count)"
        '排除目录: build/、.gradle/、.venv/'
        ''
        '--- git ---'
        $headInfo
        ''
        '--- 还原 ---'
        '.\scripts\snapshot.ps1 -Restore "' + $snapshotDir + '" -Force'
    )
    $headPath = Join-Path $snapshotDir 'HEAD.txt'
    Set-Content -LiteralPath $headPath -Value ($meta -join $newline) -Encoding UTF8

    Write-Host "归档完成: $zipPath"
    Write-Host ("归档大小: {0:N1} MB" -f ((Get-Item -LiteralPath $zipPath).Length / 1MB))
    Write-Host "HEAD 记录: $headPath"
    Write-Host '--- git ---'
    Write-Host $headInfo
    return
}

$snapshotDir = (Resolve-Path -LiteralPath $Restore).Path
if (-not (Test-Path -LiteralPath $snapshotDir -PathType Container)) {
    throw "快照目录不存在：$Restore"
}
$snapshotZip = Get-ChildItem -LiteralPath $snapshotDir -Filter '*.zip' -File | Select-Object -First 1
if (-not $snapshotZip) {
    throw "快照目录中没有 .zip 文件：$snapshotDir"
}

$archive = [System.IO.Compression.ZipFile]::OpenRead($snapshotZip.FullName)
$names = @()
try {
    foreach ($entry in $archive.Entries) {
        if ($entry.Name) { $names += $entry.FullName }
    }
}
finally {
    $archive.Dispose()
}

Write-Host "快照目录: $snapshotDir"
Write-Host "压缩包:   $($snapshotZip.FullName)"
Write-Host "目标目录: $projectRoot"
Write-Host "文件数:   $($names.Count)"

$headFile = Join-Path $snapshotDir 'HEAD.txt'
if (Test-Path -LiteralPath $headFile) {
    Write-Host '--- 快照记录的 git 状态 ---'
    Get-Content -LiteralPath $headFile -Encoding UTF8 | ForEach-Object { Write-Host "  $_" }
}

if (-not $Force) {
    Write-Warning '未加 -Force：只展示将要还原的内容，没有写入任何文件。'
    Write-Host ('确认后执行: .\scripts\snapshot.ps1 -Restore "' + $snapshotDir + '" -Force')
    return
}

$archive = [System.IO.Compression.ZipFile]::OpenRead($snapshotZip.FullName)
try {
    foreach ($entry in $archive.Entries) {
        if (-not $entry.Name) { continue }
        $target = Join-Path $projectRoot $entry.FullName
        $parent = Split-Path -Parent $target
        if (-not (Test-Path -LiteralPath $parent)) {
            New-Item -ItemType Directory -Path $parent -Force | Out-Null
        }
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $target, $true)
    }
}
finally {
    $archive.Dispose()
}

Write-Host "已还原 $($names.Count) 个文件到 $projectRoot"
