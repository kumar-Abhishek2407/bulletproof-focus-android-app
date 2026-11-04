param([switch]$Lint)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.tools/gradle-home'
$env:ANDROID_USER_HOME = Join-Path $projectRoot '.tools/android-user'
$env:ANDROID_HOME = Join-Path $projectRoot '.tools/android-sdk'
$gradle = Join-Path $projectRoot '.tools/gradle-8.11.1/bin/gradle.bat'
if (-not (Test-Path $gradle)) { throw 'Run tools/bootstrap.ps1 first, or use gradlew with your Android Studio SDK.' }
$tasks = @('assembleDebug')
if ($Lint) { $tasks += 'lintDebug' }
& $gradle --no-daemon --console=plain @tasks
if ($LASTEXITCODE -ne 0) { throw 'Android build failed' }
