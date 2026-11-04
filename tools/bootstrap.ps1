$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$toolRoot = Join-Path $projectRoot '.tools'
New-Item -ItemType Directory -Force $toolRoot | Out-Null
$gradleVersion = '8.11.1'
if (-not (Test-Path "$toolRoot/gradle-$gradleVersion/bin/gradle.bat")) {
    curl.exe --fail --location --retry 2 --output "$toolRoot/gradle.zip" "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
    if ($LASTEXITCODE -ne 0) { throw 'Gradle download failed' }
    curl.exe --fail --location --output "$toolRoot/gradle.sha256" "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip.sha256"
    if ($LASTEXITCODE -ne 0) { throw 'Checksum download failed' }
    $expected = (Get-Content "$toolRoot/gradle.sha256" -Raw).Trim()
    if ((Get-FileHash "$toolRoot/gradle.zip" -Algorithm SHA256).Hash.ToLower() -ne $expected) { throw 'Gradle checksum mismatch' }
    Expand-Archive -LiteralPath "$toolRoot/gradle.zip" -DestinationPath $toolRoot -Force
}
$env:GRADLE_USER_HOME = Join-Path $toolRoot 'gradle-home'
& "$toolRoot/gradle-$gradleVersion/bin/gradle.bat" --no-daemon wrapper --gradle-version $gradleVersion --distribution-type bin
if ($LASTEXITCODE -ne 0) { throw 'Gradle wrapper generation failed' }
