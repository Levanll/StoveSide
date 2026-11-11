# 构建脚本：所有 Gradle 缓存、构建产物、下载物一律不落 C 盘。
# 用法： .\scripts\build.ps1 [-Task assembleDebug]
param([string]$Task = "assembleRelease")

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

# Gradle 用户目录与发行版都在 F:，避免往 C:\Users 写缓存
$env:GRADLE_USER_HOME = "F:\android-dev\gradle-home"
$gradle = "F:\android-dev\gradle-dist\gradle-8.9\bin\gradle.bat"

if (-not (Test-Path $gradle)) { throw "Gradle 未找到：$gradle" }
if (-not (Test-Path "F:\android-dev\sdk")) { throw "Android SDK 未找到：F:\android-dev\sdk" }

Push-Location $root
try {
    & $gradle $Task --offline --no-daemon
} finally {
    Pop-Location
}
