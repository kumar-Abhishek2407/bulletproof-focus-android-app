$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
New-Item -ItemType Directory -Force .test-build | Out-Null
javac -encoding UTF-8 -d .test-build app/src/main/java/com/stillfocus/app/DomainRules.java app/src/main/java/com/stillfocus/app/DomainIndex.java app/src/main/java/com/stillfocus/app/DnsPacket.java app/src/main/java/com/stillfocus/app/SessionDeadline.java app/src/main/java/com/stillfocus/app/ForegroundApp.java app/src/main/java/com/stillfocus/app/SetupState.java app/src/main/java/com/stillfocus/app/SessionMode.java app/src/main/java/com/stillfocus/app/DailyWindow.java app/src/main/java/com/stillfocus/app/AppSchedule.java app/src/main/java/com/stillfocus/app/TimedBlock.java app/src/main/java/com/stillfocus/app/CallDomains.java app/src/main/java/com/stillfocus/app/BlockedAppExit.java app/src/main/java/com/stillfocus/app/RecoveryBackoff.java app/src/main/java/com/stillfocus/app/DnsTtl.java app/src/main/java/com/stillfocus/app/SiteRules.java app/src/main/java/com/stillfocus/app/DnsPolicy.java tests/CoreTests.java
if ($LASTEXITCODE -ne 0) { throw 'Core test compilation failed' }
java -cp .test-build CoreTests
if ($LASTEXITCODE -ne 0) { throw 'Core tests failed' }
