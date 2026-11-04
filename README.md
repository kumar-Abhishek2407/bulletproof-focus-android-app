# Still Focus for Android - normal and strict modes (0.17.0)

Native Android focus app with timed website blocks, app focus screens and bundled adult-domain filtering. No account, analytics, ads or backend.

## Optional Accessibility blocking (0.17.0)

1. Install the current APK over your existing version; keep Normal mode selected.
2. Open **Apps** or **Settings and help**, then **Responsive app blocking > Set up Accessibility blocking**.
3. Read the disclosure and tap **Agree and open settings**. In Android Accessibility settings, select **Still Focus app blocking** and enable it.
4. Return to Still Focus and check that the card says **Accessibility connected**. Keep Display over other apps allowed for the focus cover. Existing Usage access can remain enabled for fallback.
5. Start a two-minute timer for a nonessential app. Test opening it from both its icon and Recents, including repeated attempts. It should show the break message and request Home immediately on a window-change event. Verify access returns at expiry.
6. To stop this optional feature, tap **Turn off Accessibility blocking** or disable the service in Android Accessibility settings. Usage polling resumes as fallback when available.

This service receives window package names, not screen contents. Window-content retrieval and gesture capabilities are disabled; no screen text or passwords are read, and events are not saved or transmitted. It uses the system Home action only for selected normal-timed/scheduled apps, retaining call and lock-screen exceptions. Android may restrict Accessibility for sideloaded builds; review its warning and use the app's permission help. Keep Play Protect enabled. Play distribution requires separate Accessibility declaration/review; this build is not Play-approved.

Test service enable/disable, revoked overlay permission, reboot/reconnection, incoming calls, expiry, and repeated Recents reopening on the target phone. This is event-driven interception, not process suspension or removal from memory. Device testing is pending.

## Current app blocking behavior (0.17.0)

Normal mode is restored for app timers and schedules, and is the default for fresh installs. Existing mode preferences are preserved; select Normal on the Apps page to test without device-owner setup. Install this APK over 0.14.0 or 0.15.0 to preserve data.

On detecting a selected app, Normal mode immediately covers it, shows "[App] is on a break. Available in MM:SS", and requests Home without the former one-second delay. A short Android toast keeps the message readable after returning Home. The same logic runs on detected Recents reopens. Android may delay usage events or deny Home requests; the cover remains the fallback.

Normal mode cannot prevent the app process from starting, remove another app's Recents task, or guarantee background playback stops. Strict mode remains optional and requires device-owner setup. Its suspension checks are retained. This release changes neither DNS filtering nor call exceptions. Physical-device acceptance: test repeated launcher/Recents opens, expiry, permission revocation and incoming calls.

## Usage guide (0.17.0)

### 1. Install or update

Install [StillFocus-0.17.0.apk](artifacts/StillFocus-0.17.0.apk), then open **Still Focus**. Install over your existing version to retain settings, schedules and timers; do not uninstall first. This is a debug-signed testing build. If Android blocks installation, read the warning and see [Play Protect and distribution](#play-protect-and-distribution).

#### USB installation on Windows (tested installation route)

On the Lava Curve 5G running Android 16, installing 0.17.0 from WhatsApp was blocked by Play Protect. The user subsequently installed the same build successfully using USB ADB (`Success`). This confirms installation only; Accessibility blocking and Strict enforcement still need device testing.

1. Enable USB debugging in the phone's Developer options, connect it to your computer, unlock it and approve the computer's debugging prompt.
2. Open PowerShell in your extracted `platform-tools` folder.
3. Check the connection:

```powershell
.\adb.exe devices
```

The phone must show `device`. If it shows `unauthorized`, unlock the phone and approve the debugging prompt before continuing. If several devices are listed, select the intended phone with `-s SERIAL` on each command.

4. Install/update the APK from the computer:

```powershell
.\adb.exe install -r "D:\bulletproof-focus-android-app\artifacts\StillFocus-0.17.0.apk"
```

Wait for `Success`. The `-r` option retains existing app data when updating a compatible build. Do not uninstall first. If PowerShell says `adb is not recognized`, use **`.\adb.exe`**, including the leading dot and backslash; PowerShell does not automatically run executables in the current folder.

5. Open Still Focus, select **Normal**, and follow [Optional Accessibility blocking](#optional-accessibility-blocking-0170). This needs no device-owner provisioning, account removal or root.

Keep Play Protect enabled. If installation is rejected, retain the exact error for diagnosis instead of disabling verification. USB installation is a development-testing route, not a requirement for a future Play-distributed app. See [Android ADB documentation](https://developer.android.com/tools/adb).

#### Play Protect installation warning versus restricted permissions

- **"App blocked to protect your device" during installation:** Google documents this warning for internet-sideloaded apps declaring sensitive permissions, including Accessibility. An optional feature still declares its service in the APK. See [Google's warning guidance](https://developers.google.com/android/play-protect/warning-dev-guidance).
- **"Restricted setting" after installation:** this concerns granting a special permission to an installed app. Follow the permission instructions below. It is a different step from an installation block.
- **Installing through Google Play:** requires preparing a Play release and satisfying applicable reviews, including the Accessibility declaration and disclosure/consent requirements. This project has not obtained Play approval. See [Accessibility API requirements](https://support.google.com/googleplay/android-developer/answer/10964491).

### 2. Complete first-time permissions

On the **Permissions** page, open each permission and approve it in Android's screen:

1. **Website protection**: approve the VPN connection request.
2. **Usage access**: select Still Focus and allow usage access so it can detect a selected app opening.
3. **Display over other apps**: allow Still Focus to display its focus screen over selected apps.

Return to Still Focus after each approval. Notifications are optional. Once the required permissions are granted, Home opens automatically and setup completion is saved. Later launches skip this page. If you revoke a permission later, use **Settings and help** or the affected feature's restore control to grant it again.

If Android says a permission is restricted for the sideloaded APK, review the warning. For a build you trust, Android may offer **Settings > Apps > Still Focus > three-dot menu > Allow restricted settings**; then retry the permission. Menu names and availability vary by phone. Keep Play Protect enabled.

### 3. Choose Normal or Strict

| Mode | Setup | Behavior during a block |
| --- | --- | --- |
| **Normal** | VPN, Usage access and overlay permissions; no root or device-owner setup | Covers selected apps and requests Home immediately on detection. Android Settings can still stop or uninstall Still Focus. |
| **Strict** | Android 11+ and device-owner provisioning; no root | Suspends selected apps and applies uninstall/user-control restrictions while a strict block is active. |

For everyday testing, choose **Normal** using the mode control on the blocking page. Strict cannot work just by selecting it: follow [Strict setup](STRICT_MODE_SETUP.md) first, using the current APK linked above rather than the older APK named in that guide. An error about existing accounts means Android refused device-owner provisioning; it does not mean normal mode needs account removal. Provisioning may require a reset, so use a dedicated test device where possible.

#### Prevent uninstalling during a timer (Strict only)

Normal mode, including optional Accessibility blocking, does **not** guarantee uninstall prevention. Strict mode uses Android device-owner controls; enabling Accessibility or installing through ADB does not enable those controls.

1. Read [Strict setup](STRICT_MODE_SETUP.md) before provisioning. Prefer a spare test phone; back up personal data before considering account removal or a reset.
2. With the installed phone connected and authorized, run this in the PowerShell `platform-tools` folder:

```powershell
.\adb.exe shell dpm set-device-owner com.stillfocus.app/.FocusAdminReceiver
```

3. If provisioning succeeds, reopen **Settings and help > Strict mode setup** and verify **Device owner: ready**.
4. Choose **Strict** on the blocking page and start a short timer, such as two minutes. In Android **Settings > Apps > Still Focus**, verify that uninstall and user controls are restricted while it runs.
5. After all strict timers and active strict schedule windows end, disable future strict schedules. Open **Settings and help > Remove device-owner setup**, then uninstall normally if desired. Timer expiry releases timer policies; it does not itself remove device-owner enrollment.

**If Android reports `Not allowed to set the device owner because there are already some accounts on the device`:** the command reached Android correctly, but provisioning was refused. This is the currently reported result on the user's Lava phone, so Strict uninstall protection has not been enabled. Repeating the command or enabling Accessibility does not fix account eligibility. Provisioning may require account removal or a factory reset depending on the device; neither is performed by Still Focus. Do not remove accounts or reset a personal phone merely to test Normal mode.

Strict protection covers normal Android app controls, not root, privileged debugging or factory resets. Device-specific enforcement must be tested after successful provisioning.

### 4. Block an app for a few minutes

1. On Home, tap **Open apps**.
2. Tap **Choose applications**. Search for the app and tap its name/icon row to select it.
3. Tap **Done**. Check the selected apps and choose **Normal** for an ordinary phone.
4. Enter the duration in minutes, for example **2** for a quick YouTube test.
5. Tap **Start app block**, then **Start session** in the confirmation.
6. Open the selected app. In normal mode, a focus screen should appear and Still Focus should immediately request a return Home. Once the timer expires, access returns.

Each selected app gets its own deadline. To give another app a different duration, return to **Open apps**, select that app and start another block. You do not have to wait for the first timer to finish. Calling apps are excluded from selection to reduce interference with calls.

There is no in-app cancel/shorten action for active timers. Normal mode does not stop the other app's process: background audio, picture-in-picture and Recents previews may remain visible. In 0.13.0, the focus screen shrinks from all four corners toward the bottom centre, 14% above its bottom edge, after Home is detected.

### 5. Block websites

1. Tap **Open websites** on Home.
2. Enter one website domain or URL per line, for example `youtube.com` and `reddit.com`. Subdomains are included.
3. Choose the mode and duration, then tap **Start website block** and confirm.
4. Close and reopen the browser, then try the selected website.

Website timers are independent too: start separate blocks to use different durations. For browsers to use this app's DNS filter, turn **Android Private DNS off** and disable **Secure DNS** in Chrome/Brave. Use **Browser setup instructions** on the website page for help. Cached DNS, existing connections and other encrypted DNS paths can still bypass filtering.

### 6. Schedule apps every day (example: 9 PM to 9 AM)

1. Open **Open apps > Schedules > Add schedule**.
2. Give the schedule a name and use **Choose apps** to select apps by name/icon.
3. Set **From** to **9:00 PM** and **Until** to **9:00 AM**.
4. Choose Normal or a provisioned Strict mode, then save the schedule enabled.

This repeats daily in the phone's local timezone and ends the next morning. Normal schedules can be edited, disabled or deleted. Active strict schedules cannot be changed until their window ends. Overlapping timers and schedules keep an app blocked until all applicable blocks end.

Use **Allow precise scheduling** on the Schedules page if offered, and grant Android's **Alarms & reminders** access. Without it, schedule boundaries may be delayed during sleep.

### 7. Enable ad blocking

Open **Settings and help**, tap **Ad blocking: Off** and approve the AdGuard DNS explanation. The button then shows **Ad blocking: On**. Allowed DNS queries go to AdGuard's public HTTPS resolver; enabling this shares those queries with AdGuard. No manual Private DNS hostname is needed.

Ad blocking and bundled domain protection run independently of focus timers; finishing a timer does not switch them off. Truecaller is included in DNS filtering in 0.12.0 but remains protected from app timers. Close and reopen Truecaller after enabling protection. Cached ads, first-party ads and app-owned encrypted DNS may still appear; zero ads is not guaranteed. Known calling domains and detected calls retain safety exceptions. Test incoming/outgoing and Wi-Fi calls on your phone after enabling the feature.

### 8. Keep protection running

From Home, tap **Keep protection running**, or open **Settings and help** and scroll to the section with that title.

1. Open the Android VPN settings control and enable **Always-on VPN** for Still Focus.
2. **Leave Block connections without VPN OFF.** This app uses a DNS-only VPN; that lockdown setting can block ordinary internet access.
3. In battery/app settings, allow unrestricted battery/background use for Still Focus.
4. Keep Android Private DNS and browser Secure DNS off when using this filter. Another VPN can replace Still Focus's VPN.
5. If protection has stopped, reopen Still Focus and tap **Restart protection service** in this section.

Recovery retries network failures, but revoked permissions, force-stop and manufacturer power restrictions can still interrupt protection. A VPN key icon alone does not prove an app's DNS queries are being filtered.

### 9. Read the status when ads still appear

Open **Still Focus > Settings and help**, then scroll to **Keep protection running**. Below its controls is a text status area showing connection uptime, DNS requests and a line beginning **Ad blocking:**. This is inside Still Focus, not Truecaller.

| Status text | Meaning / next step |
| --- | --- |
| **off** | Enable ad blocking using the button above. |
| **enabled; waiting for a DNS request** | No successful AdGuard response has been recorded yet. Open a website and recheck; check Private DNS/browser Secure DNS if requests are not arriving. |
| **AdGuard responding** | The service has recorded a successful resolver response. This does not prove every app request or ad is filtered. |
| **call bypass active** | Android reports a calling/audio mode for which the app temporarily bypasses AdGuard. Recheck after the call ends. |
| **AdGuard unavailable; filtered DNS requests are waiting...** | Ordinary filtered DNS requests fail instead of falling back to unfiltered DNS. Check connectivity and restart protection. Turning ad blocking off restores ordinary network DNS. |

If ads persist, report the installed app version (at the bottom of Settings), the full **Ad blocking:** line, whether the DNS request count increases, and whether you were on a call. A screenshot of this Still Focus status area is also useful. Avoid sharing contact details from other apps.

## How app blocking works

With explicit special-access permission, the VPN foreground service polls Android UsageEvents approximately every 200 ms to track the foreground package name in memory. It displays an application overlay only over user-selected apps during an active normal session. An optional, consented Accessibility service can instead receive app-window events and request the system Home action. No screen text, password fields or page contents are retrieved. Usage-event history is not saved or sent off-device. System navigation and Settings remain accessible; the overlay does not appear over the lock screen.

The overlay blocks interaction, not the app process or network. Background audio, existing downloads and picture-in-picture may continue. Split-screen behavior, apps that hide third-party overlays, permission revocation and manufacturer power management can limit blocking. There can be a short delay before the focus screen appears. These behaviors need testing on your target phone.

## Website and adult-domain filtering

The APK bundles **937,820 suffix-domain entries and 3 exact hosts**, imported from `D:\bulletproof-focus-extension`. Domain boundaries and exact-host semantics are preserved. Adult-domain filtering starts after first-run VPN consent and has no in-app off switch. No app can obtain Android VPN permission or activate itself automatically at installation.

This is a **DNS-only split-route VPN**, not a full packet-forwarding VPN. By default allowed DNS queries use the physical network configured DNS servers over UDP. Optional AdGuard DNS sends allowed queries over HTTPS to https://dns.adguard-dns.com/dns-query, with ordinary DNS requests failing on provider failure when ad blocking is enabled (calling exceptions remain). Enabling this shares DNS queries with AdGuard. No system Private DNS settings are changed. Names are not logged or sent to an app server. Other network traffic follows its normal route.

Encrypted DNS, alternate DNS paths, proxies, direct IP connections, DNS caches and existing connections can bypass domain filters. No static list can classify all adult content; mixed-content platforms, new domains and false positives remain limitations. DNS-over-TCP to the virtual resolver is not implemented, so unusually large/truncated DNS replies can fail. Network DNS resolvers requiring encryption are unsupported in this mode. Only one VPN can run per Android user.

You may enable Android **Always-on VPN** for recovery. **Leave "Block connections without VPN" OFF**: lockdown is incompatible with this DNS-only split-route design and blocks ordinary internet access. Reboot recovery starts after unlock, not during direct boot. Manufacturer battery settings may interrupt the service.

## Timers and upgrades

Sessions last 1 minute to 365 days and persist across app restarts. Monotonic elapsed time prevents clock changes from shortening or extending a timer within a boot; reboot recovery uses the saved calendar deadline. In normal mode, system clock changes around a reboot can affect that deadline.

Version 0.6.0 adds a Protection mode selector to both timer pages and Settings. Strict was the initial preference in 0.6.0. In 0.17.0, Normal is the fresh-install default; existing preferences are preserved. Each item saves its mode until expiry. The mode selector applies to new timers; normal and strict timers can coexist. Strict requests never fall back to normal. A strict session activates uninstall/data/force-stop controls, date/time restrictions and selected-app suspension only after device-owner readiness is confirmed. If upgrading from a previously provisioned device-owner build with an active strict session, its stored mode defaults to legacy strict and its existing policies are preserved until expiry. Then use **Remove device-owner setup** if shown to remove the old enrollment. Normal sessions do not need enrollment; the release action is allowed during a normal session. The app does not reset your phone.

## Build and verification

Android Studio: JDK 17, SDK 35, Gradle 8.11.1. Android 8+ supported. In PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File tools/bootstrap.ps1
powershell -ExecutionPolicy Bypass -File tools/install-sdk.ps1
powershell -ExecutionPolicy Bypass -File tools/build.ps1 -Lint
powershell -ExecutionPolicy Bypass -File tools/test.ps1
python -m unittest discover -s tests -p 'test_*.py'
```

The SDK helper downloads checksum-verified tools into ignored `.tools`, accepts development SDK licenses and writes ignored `local.properties`. Debug signing is for testing. Standard Gradle output: `app/build/outputs/apk/debug/app-debug.apk`.

Core tests cover domain/IDN validation, all bundled suffix lookups, malformed DNS packets, response validation/checksums, monotonic timers/reboot deadlines and foreground-event ordering. Manifest tests constrain the optional Accessibility service to window events without content retrieval or gestures, and reject SMS/notification-listener capabilities. Build/lint and APK signature verification do not establish Google approval or real-device behavior.

Manual acceptance on an ordinary, unprovisioned phone:

1. Install over 0.2.0. Verify no device-owner requirement is displayed and website-only timers need only VPN permission.
2. Try selecting YouTube without special access: get actionable instructions for the two permissions, not a device-owner error.
3. Grant Usage access and Display over other apps. Start a two-minute YouTube session. Open YouTube: verify the focus screen appears, Home/Settings remain accessible, and unrelated apps work.
4. At expiry, verify the focus screen disappears and YouTube becomes usable. Adult filtering remains active. Check background audio/PiP separately; these are documented limitations.
5. Revoke either app-blocking permission during a session: the overlay must disappear. Regrant and reopen the selected app to verify recovery.
6. Rotate/reopen Still Focus, switch Wi-Fi/mobile networks, reboot/unlock and verify session recovery. Test no-network, VPN revocation and browser Secure DNS paths.
7. On a separately managed test device only, upgrade an active legacy strict session and verify restoration of its suspension/uninstall policies at expiry.

## Blocklist refresh and notices

```powershell
python tools/import_blocklists.py D:\bulletproof-focus-extension
powershell -ExecutionPolicy Bypass -File tools/test.ps1
```

Source hashes/counts are in `app/src/main/assets/blocklist-metadata.json`. Source revisions, MIT and Unlicense notices are included in `blocklist-notices`. No background blocklist download is performed.

## Play Protect and distribution

Version 0.2.0 removed the original Accessibility service after the reported sensitive-permission sideload warning. Version 0.3.0 keeps it removed and uses explicit Usage access / Display over other apps grants for normal app blocking. Keep Play Protect enabled. Installation still depends on Android/Google verification; if blocked, inspect the full warning and follow the documented review process. No package renaming or security bypass is used.

- [Google's warning guidance](https://developers.google.com/android/play-protect/warning-dev-guidance)
- [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager)
- [Application overlays](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY)
- [VPN lifecycle and consent](https://developer.android.com/develop/connectivity/vpn)

Play distribution and production signing require separate review. This repository does not claim Play approval or zero-bypass enforcement.

## Release history

The sections below describe earlier releases at the time they shipped. Later releases supersede earlier behavior, including DNS fallback and animation behavior. Use the usage guide above for 0.13.0 instructions.

## Version 0.4.0 validation checklist

- Fresh install: see Permissions first; grant one permission at a time, deny/retry, and return from Android Settings. Granted rows become inactive. Completing the last required permission opens Home automatically.
- Existing grants on upgrade: go straight to Home. Restart/rotate after completion; setup must not return. Revoke a permission after completion: Home remains available, and only the affected blocking page offers recovery.
- Home: exactly two primary choices, Block apps and Block websites. Verify system Back/page Back follow the parent hierarchy, including the app-picker child page.
- Apps: search/select YouTube, return to Block apps, choose two minutes, start and verify countdown. Website drafts must not enter the app session. Test overlay behavior and expiry on a real phone.
- Websites: enter example.com, navigate away/back and rotate, then start a timer. App selections must not enter the website session. Verify browser filtering with Secure DNS off.
- During an existing session, new session controls point to the current countdown. At expiry, completed-session text and start controls update automatically without restarting the app.

Host setup-state tests cover partial grants, one-time completion, permission revocation after completion, restart and existing-grant upgrades. These do not replace on-device permission/dialog/navigation checks.

## Version 0.5.0: simple app selection

Application rows now show the installed app icon and readable name, with a checkbox on the right. Package identifiers are not displayed. Tap anywhere on a row to toggle it; selected rows are highlighted. Search filters the same list without clearing selections. A fixed bottom **Done** button shows the selected count and remains visible while scrolling. The Block apps page previews selected apps with their icons too.

Check on a phone: search YouTube, tap its row and checkbox separately (each should toggle once), scroll while keeping Done visible, return to Block apps and verify the icon/name preview. Rotate or use Back and verify selection is retained. Settings shows the installed version dynamically so an older APK can be identified.

## Version 0.6.0: optional strict sessions

Read [STRICT_MODE_SETUP.md](STRICT_MODE_SETUP.md) before provisioning. The app provides Settings and help > Strict mode setup with status, clear steps and a copyable ADB command. Provisioning and resets are not performed by the app.

The first-run setup can recognize device-owner readiness as an alternative to Usage access / overlay for strict app suspension. Its strict-setup link is accessible before the general permission setup finishes. VPN consent remains required. Completed onboarding still never reopens automatically.

Strict session start validates owner status, persists the chosen mode, applies the policies, and checks that Android reports uninstall blocking plus disabled user controls before reporting success. It retains the existing partial-start rollback. Normal mode only requires the overlay/usage permissions for selected applications. At expiry, suspension and timer policies restore their previous state. Device-owner enrollment remains until explicitly removed after the strict session.

Verification: test both modes before and after provisioning on a dedicated phone, strict start refusal without an owner, uninstall/force-stop/data-clear denial during the timer, inability to change mode or remove ownership mid-session, reboot recovery and expiry cleanup. Host tests cover mode permission gating and no-fallback behavior; actual device-policy enforcement remains a hardware acceptance check.

## Version 0.8.0: minimal UI and daily schedules

The home screen uses compact Apps / Websites cards and neutral protection status. Filter-category references are removed from app screens, permission copy, service status and notifications. The bundled domain filtering engine and notices remain unchanged.

**Apps > Schedules > Add schedule** opens a daily schedule editor. Select apps (icons + names), choose From / Until with Android time pickers and save. The default is 9 PM to 9 AM, every day, in the phone's local timezone. Overnight ranges are supported; equal start/end is rejected. Normal schedules can be edited, switched off or deleted at any time. They use the normal app overlay and require its two special-access permissions. Future enabled schedules are stored locally and survive app restarts/reboots.

Strict schedules require existing device-owner setup. They suspend the union of selected apps and apply uninstall/data controls only while at least one strict timer or schedule is active. Active strict schedules cannot be edited, disabled or deleted until their window ends. Other schedules/timers can overlap: ending one must not restore apps still covered by another. Pre-existing app suspension is retained. Enabled strict schedules must be disabled before removing enrollment. The app rejects a set of strict schedules covering all 24 hours so a daily exit window remains available.

An ongoing foreground service checks active windows, and an alarm re-arms for the next boundary. Enable **Alarms & reminders** through the optional **Allow precise scheduling** control for better behavior during sleep. If unavailable or revoked, the app uses inexact alarms and displays a delay notice. It recomputes boundaries after timezone/time changes, reboot, update and exact-alarm grants. Exact alarm access, battery settings and phone vendor behavior still require on-device verification. Force-stop/revoked VPN/usage/overlay permissions can interrupt normal schedules. Schedules start after unlock following reboot; there is no direct-boot enforcement.

Host tests cover start-inclusive/end-exclusive boundaries, overnight windows, overlap unions, disabled entries, independent strict/normal windows, daylight-saving gaps/overlaps and continuous strict coverage detection. UI tests guard against reintroducing filter-category wording. Manual acceptance should cover real 2-minute-near-future windows, 9 PM-9 AM across midnight, overlapping timers/schedules, process restart, reboot, timezone changes and strict-policy restoration on a separately managed phone.

[Android alarm scheduling guidance](https://developer.android.com/develop/background-work/services/alarms)


## Version 0.8.0: separate timers, Recents tracking and optional ad DNS

Usage events now include the activity name: a late pause from an old activity cannot clear a newer activity in the same app. A rolling query window catches delayed events, and a new resume reattaches the overlay after Recents. Normal mode cannot suppress Recents snapshots, stop background playback, or override apps/system screens that suppress overlays. Strict suspension remains the stronger option. Device testing is required to confirm the reported Lava Recents path.

Settings > Ad blocking enables AdGuard public DNS after an explicit disclosure. Off by default; turning it on/off requires no tunnel restart. DNS cannot reliably remove YouTube video ads or same-domain ads. Provider failures fall back to network DNS, so ad blocking is best effort. See [AdGuard public DNS](https://adguard-dns.io/en/public-dns.html).

Phone/IMS, the default dialer, discovered Android ConnectionService providers and known communication apps are excluded from the VPN and app selection. Carrier DNS exceptions bypass local filters; communication DNS exceptions bypass AdGuard. Call audio mode suppresses normal overlays and ad DNS. These measures reduce interference but cannot guarantee uninterrupted carrier, Wi-Fi or app calls on every phone. No phone-state permission is requested. Carrier/OS paths, encrypted DNS and caching remain outside this app's control. Reopen protection after changing default dialer or installing a new calling app.

Acceptance on Lava Android 16: start YouTube for 2 minutes, another app for 5, and a website for 3. Repeat Home/Recents return to YouTube several times; verify overlay returns and each item releases at its own deadline. Check mobile calls, incoming calls, Wi-Fi calling and a communication-app call with ad DNS off/on, while a timer and daily schedule are active. If calls regress, switch ad blocking off immediately and restore the phone's previous network configuration. Existing Android Private DNS must be off to use the VPN's ad resolver.


## Version 0.8.1: normal-mode Home redirect

When UsageEvents identifies a normal-timed app in the foreground, Still Focus now requests Home as well as displaying the overlay. A new resume triggers a fresh Home request immediately; an unacknowledged request is retried at most once per 1.5 seconds. Call-audio, lock-screen and protected calling-app exclusions run before this behavior. No tasks are removed and no system navigation is blocked. Android may deny/delay the Home request. Recents thumbnails and media playback remain outside normal-mode control. The UI now says access is blocked rather than claiming the process is paused. Strong suspension requires device-owner strict mode. This change is not yet validated on the Lava device.

Install `artifacts/StillFocus-0.8.1.apk` over the existing APK. Repeat the reported YouTube Music Recents path during a two-minute normal timer, then verify another app and Phone remain usable. Background music may continue; check strict mode separately on a provisioned test device for suspension behavior.


## Version 0.9.0: color and motion

A violet gradient welcome card, lavender app cards, peach website cards and mint active blocks replace the green palette. Shared rounded controls include touch ripples and a small press animation. App selections have tinted checkboxes/backgrounds; duration presets highlight the selected value, including typed changes. Main pages and the schedule editor/picker slide and fade in the navigation direction. Activity transitions share the same style; in-page movement follows Android animator accessibility settings and does not replay on timer ticks. The normal blocking screen uses the same palette. Native system permission screens retain their Android appearance.

Install `artifacts/StillFocus-0.9.0.apk` over the existing version. Device acceptance: navigate Home > Apps > Choose apps > Back, verify selected names/icons and drafts survive, type/pick a duration and verify its highlight, navigate schedules/editor/picker, and check active timers continue unchanged. Repeat with animations disabled and with large system text. UI has been compiled/linted; visual behavior still requires real-device review.


## Version 0.10.0: persistent network protection

The tunnel now reconnects after read/write failures or EOF instead of stopping its service. Retries are bounded to 1-30 seconds, reset after a stable connection, and never depend on timer expiry. Every asynchronous DNS response belongs to the tunnel that received its query; old workers cannot write into a replacement tunnel. VPN revocation stops recovery. START_STICKY and Android Always-on VPN handle eligible process restarts; force-stop, revoked consent and vendor restrictions cannot be overridden. The Settings restart action now rebuilds a live tunnel. Opening the app also retries a failed start instead of retaining a stale start-request flag. Boot recovery runs even if app-policy or alarm reconciliation fails.

AdGuard DNS is tried on available physical networks before ordinary network DNS fallback. DoH timeouts are three seconds. Provider failures appear in Settings and the ongoing notification. Fallback DNS record TTLs are capped at 30 seconds (EDNS flags preserved) so a brief failure does not leave unfiltered results cached for hours. This remains fail-open for connectivity: ads may appear during provider outages and calling bypasses. DNS caching and existing connections in browsers may outlive DNS TTLs. Local bundled rules still apply independently of AdGuard availability.

The requested `xx` / `xh` rules block DNS names when a non-TLD label begins with either prefix, including `www` and other subdomains of those names. These broad lexical rules can also block unrelated sites; they do not classify content. Carrier calling-domain exceptions retain priority. Category details remain absent from the UI.

Use Settings > Keep protection running: enable Android Always-on VPN for Still Focus, leave Block connections without VPN OFF, allow unrestricted battery/background use, and switch Android Private DNS and browser Secure DNS OFF. No device-owner setup or account removal is required for these controls. The diagnostics show uptime, query count/recency, provider fallback, Private DNS conflicts and the last interruption without storing domain names or browsing history. Android Always-on behavior: https://developer.android.com/develop/connectivity/vpn ; battery controls: https://developer.android.com/training/monitoring-device-state/doze-standby .

Device acceptance (still required on Lava Android 16): verify filtering with no app timers; leave the phone idle for over two hours; recheck after Wi-Fi/mobile changes and an unlocked reboot. Exercise Restart while queries are outstanding, and confirm later replies use the new tunnel. Revoke VPN consent and confirm recovery stops. Simulate an AdGuard outage and verify the fallback warning, ordinary connectivity and resumed filtering when reachable. Check mobile/Wi-Fi calls and calling apps. A visible VPN key alone does not prove browser queries are passing through the filter.


## Version 0.11.0: softer interface and a two-second focus transition

Apps and Websites now share the same pale periwinkle card treatment. The hero uses a subtle pastel gradient with dark text; buttons use a muted solid accent. Schedules and the focus overlay share the softer palette.

Normal mode covers the selected app immediately, waits two seconds while it remains detected in the foreground, then requests Home. The focus screen enters gently and fades once Home is detected; the Home transition requests Android's custom animation. The two seconds are not an interactive grace period. Phone/call, lock-screen, expiry and foreground changes cancel pending exits. There is no delayed callback that can eject the user from another app. Android still controls other apps' processes, playback and task previews; strict suspension is unchanged. Android animation settings are respected.

Domain filtering now also blocks names containing `sex` or `fsiblog`, case-insensitively, in addition to the existing list and xx/xh rules. This covers FSIBlog names across suffixes and subdomains, but not unrelated aliases. Broad substrings may block unrelated names (for example, a domain containing Sussex). DNS sees hostnames, not URL paths or page text; encrypted DNS bypass limitations still apply.

Install `artifacts/StillFocus-0.11.0.apk` over the existing version. On-device acceptance: open a normal-blocked app, observe the focus screen for two seconds and the return Home; leave the app before two seconds and verify no later redirect; check expiry and incoming calls during the dwell; repeat with animations disabled. Review the uniform Home cards on the Lava device. Desktop timing/domain tests and build checks do not replace these device checks.


## Version 0.12.0: Truecaller DNS coverage and downward exit

Truecaller (`com.truecaller`) is now explicitly protected from app timers while being included in VPN DNS filtering. Previously the default dialer/ConnectionService discovery could exclude its entire package, including advertising lookups. Other calling-app and core phone/IMS VPN exemptions remain. The service uses the official AdGuard public HTTPS resolver, https://dns.adguard-dns.com/dns-query (https://adguard-dns.io/en/public-dns.html).

When ad blocking is enabled, ordinary queries now require an AdGuard response: provider failure returns DNS SERVFAIL instead of a successful unfiltered network-DNS answer. This prevents silent loss of ad filtering at the cost of ordinary DNS availability during an AdGuard outage. Disable ad blocking in Settings to restore network DNS if necessary. Known calling domains and detected active calls still use the call-safety bypass, so ad blocking is not guaranteed during those exceptions. Cached ads, embedded first-party ads and application-owned encrypted DNS remain limitations. Diagnostics and consent wording reflect the new behavior.

After the two-second focus-screen dwell, the Home transition requests a vertical downward exit. The overlay slides from its original position down off the bottom over 380 ms once Home is detected, without a horizontal shift, scaling, or fade. Content entry movement was removed. Reduced-motion settings and immediate removal for incoming calls, expiry and navigation elsewhere remain supported.

Device acceptance: update with ad blocking enabled, restart protection and reopen Truecaller; check its advertising panels while idle and test caller ID, incoming/outgoing mobile and Wi-Fi calls. Verify an AdGuard outage displays its failure and does not return unfiltered ordinary DNS, while call exceptions work. Repeat a blocked-app open and observe the two-second delay followed by a downward transition. Physical-device verification is pending; desktop tests do not establish zero ads or call reliability on the Lava device.

## Version 0.13.0: corner-converging exit and brighter colours

The normal-mode focus cover keeps its two-second dwell, then shrinks evenly from all four corners toward a point at 50% width and 86% height over 420 ms. A transparent overlay window reveals Home around the shrinking cover. Android receives a matching task-animation request, although the launcher controls whether it honours that request. Calls, lock, expiry and navigation elsewhere still remove the overlay immediately; disabled animator settings skip its transition.

A richer violet accent, lilac-to-rose welcome gradient, matching lavender Apps/Websites cards and fresh mint status surfaces brighten the shared palette. Dark text and solid primary buttons retain readable contrast.

Install the updated APK over the previous version. On-device checks: open a blocked app, observe the two-second dwell and corner convergence, retry from Recents, and verify immediate cleanup on expiry/calls. Check with system animations disabled. Physical-device visual verification remains required. Ad filtering is unchanged in this release.

## Version 0.14.0: one-second Home redirect

Normal mode requests Home after one second of detecting the selected app. Activity changes within the same package no longer restart the dwell or retry cooldown. Leaving the app clears the dwell; reopening from Recents starts a fresh one-second dwell. The corner-converging cover animation remains.

This is a focus cover and Home redirect, not force-stop or task removal. The selected app remains covered during the dwell rather than displaying an interactive one-second preview. Normal mode cannot remove another app's Recents card or stop its background playback. Strict device-owner suspension prevents selected apps from launching, but does not provide the requested open-then-close preview. Real-device verification of the Lava Recents path is still needed.

## Version 0.15.0

New app timers and enabled schedules require verified strict suspension. The app checks suspension on each management reconciliation instead of trusting only its cached desired state. Device-owner setup and real-device enforcement testing remain required.
