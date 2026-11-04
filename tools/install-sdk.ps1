$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$toolRoot = Join-Path $projectRoot '.tools'
$sdkRoot = Join-Path $toolRoot 'android-sdk'
New-Item -ItemType Directory -Force $sdkRoot | Out-Null
if (-not (Test-Path "$sdkRoot/cmdline-tools/latest/bin/sdkmanager.bat")) {
    curl.exe --fail --location --retry 2 --output "$toolRoot/android-cli.zip" 'https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip'
    if ($LASTEXITCODE -ne 0) { throw 'Android tools download failed' }
    if ((Get-FileHash "$toolRoot/android-cli.zip" -Algorithm SHA256).Hash.ToLower() -ne '98b565cb657b012dae6794cefc0f66ae1efb4690c699b78a614b4a6a3505b003') { throw 'SDK tools checksum mismatch' }
    Expand-Archive -LiteralPath "$toolRoot/android-cli.zip" -DestinationPath "$toolRoot/android-cli" -Force
    New-Item -ItemType Directory -Force "$sdkRoot/cmdline-tools" | Out-Null
    # Both resolved paths are inside the project's .tools directory.
    $sourcePath = [IO.Path]::GetFullPath("$toolRoot/android-cli/cmdline-tools")
    $targetPath = [IO.Path]::GetFullPath("$sdkRoot/cmdline-tools/latest")
    if (-not $sourcePath.StartsWith([IO.Path]::GetFullPath($toolRoot) + [IO.Path]::DirectorySeparatorChar) -or -not $targetPath.StartsWith([IO.Path]::GetFullPath($toolRoot) + [IO.Path]::DirectorySeparatorChar)) { throw 'Invalid SDK paths' }
    Move-Item -LiteralPath $sourcePath -Destination $targetPath
}
$env:ANDROID_HOME = $sdkRoot
$env:ANDROID_USER_HOME = Join-Path $toolRoot 'android-user'
$env:GRADLE_USER_HOME = Join-Path $toolRoot 'gradle-home'
1..100 | ForEach-Object { 'y' } | & "$sdkRoot/cmdline-tools/latest/bin/sdkmanager.bat" "--sdk_root=$sdkRoot" --licenses
if ($LASTEXITCODE -ne 0) { throw 'SDK license setup failed' }
& "$sdkRoot/cmdline-tools/latest/bin/sdkmanager.bat" "--sdk_root=$sdkRoot" 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools'
if ($LASTEXITCODE -ne 0) { throw 'SDK package install failed' }
('sdk.dir=' + $sdkRoot.Replace('\','/')) | Set-Content -Encoding ascii (Join-Path $projectRoot 'local.properties')
