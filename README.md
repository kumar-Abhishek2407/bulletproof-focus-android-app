# Still Focus for Android - normal and strict modes (0.29.1)

Native Android focus app with timed website blocks, app focus screens and bundled adult-domain filtering. No account, analytics, ads or backend.

## 0.29.1: Correct swipe direction

Install [StillFocus-0.29.1.apk](artifacts/StillFocus-0.29.1.apk). Swipe left to open the tab on the right; swipe right to open the tab on the left. This fixes the reversed gesture in 0.29.0. The first and last tabs do not wrap around.

## 0.29.0: Smoother slides and clearer buttons

Install [StillFocus-0.29.0.apk](artifacts/StillFocus-0.29.0.apk). This non-debuggable device-test build keeps the existing development signing certificate.

- Swipe right to advance to the next tab; the current page slides left and the next page enters from the right. Swipe left to go back. Slides last 280 ms without fading or bouncing.
- Action buttons share a brighter mint colour and dark text.
- Accessibility setup explanations use shorter, simpler wording while explaining screen access and consent.
- The approved Mint breathe dot appears on Active blockers when timers are active and Apps with limits when limits are saved. It gently changes colour and size, stops when the window loses focus, and respects disabled animations. [Compare the original previews](artifacts/ui-preview/active-dot-examples.html).

Payment compatibility and blocking behaviour are unchanged by this UI update. Phone testing is still needed to judge the transition on the Lava device.

## 0.28.0: Faster tabs and narrower screen access

Install [StillFocus-0.28.0.apk](artifacts/StillFocus-0.28.0.apk). This device-test APK is non-debuggable and uses the same development signing certificate as earlier versions, so it can upgrade them without clearing rules. It is not a Google Play-reviewed release.

### Navigation

The Daily limit tab no longer enumerates installed apps or loads icons before its first frame. Lists load only after opening them; package labels and icons load in the background and rows are added in small batches. Tab activities are reused instead of destroyed/rebuilt at every switch, and duplicate resume renders are removed. Directional transitions use a shorter 190 ms easing curve, with immediate ripple feedback and a restrained drag response during swipes. System animation settings are respected. Existing uniform buttons, collapsed subpage navigation, hidden active-blocker lists and hidden daily search remain.

### Payment investigation

The screen service now checks the focused window ID against a window confirmed by an allowlisted YouTube/Brave event **before** requesting its root. An unrelated focused window is rejected without reading its content. Shorts actions and browser covers recheck focus before acting. While a browser cover is visible, a lightweight metadata-only check removes it after focus moves elsewhere; it does not inspect another app's nodes.

These changes reduce actual screen access and stale-overlay exposure. They do **not** guarantee that Google Pay, Paytm or Navi accepts an enabled Accessibility service. Both original blocking services and Shorts features remain available; there is no new payment mode or hidden permission change. No payment transaction was performed during development.

See [the compatibility investigation](docs/payment-compatibility.md) for verified evidence, alternatives, limitations and the device validation still required. Regain's exact permission/trust state could not be compared because it is not installed on the connected phone. Its documented Accessibility use and Google Pay's handling of unknown apps support a trust/distribution hypothesis, not a confirmed explanation for all three payment apps.

Build this APK with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/build.ps1 -DeviceBuild -Lint
```

The default build remains a development debug APK. Neither changing the debugging flag nor using the development signing certificate grants Play verification or a payment-provider exemption.

## 0.27.0: Consistent actions and swipe navigation

Install [StillFocus-0.27.0.apk](artifacts/StillFocus-0.27.0.apk) over your current version.

- Focus uses matching **Block applications**, **Block websites**, and **Active blockers** buttons. The active list opens only after selecting Active blockers.
- Daily limit uses matching **Set daily limit** and **Apps with limits** actions. Neither list appears before selection; app search appears inside Set daily limit only. Apps with limits shows configured allowances without search. Choosing either action clears the previous search.
- The selected bottom tab is wider, has a larger label, and gently expands into place. On subpages, the four-button bar collapses into one enlarged parent-tab button. Tap it to return to the parent page, or swipe to another tab.
- Swipe left/right between Focus, Daily limit, Schedule and Settings. Horizontal gestures beginning on text fields, number pickers, sliders or system-navigation edges do not switch tabs. Vertical scrolling and multi-touch do not switch tabs. Navigation has directional slide/fade transitions and respects Android's disabled animation setting.

Blocking permissions, payment compatibility, timer enforcement and uninstall behaviour are unchanged in this UI release.

## 0.26.0: Four tabs and active-window locks

Install [StillFocus-0.26.0.apk](artifacts/StillFocus-0.26.0.apk) over the existing version; rules and permissions are retained.

```powershell
.\adb.exe install -r "D:\bulletproof-focus-android-app\artifacts\StillFocus-0.26.0.apk"
```

- **Focus:** only application/website pause actions and active blocker countdowns. Choose applications (including YouTube > Shorts only), choose a duration, and start. Website timers remain independent.
- **Daily limit:** shows configured apps and their remaining daily allowances. **Set daily limit** opens the app list; **Apps with limits** returns to configured allowances. Tap an app to adjust or remove its allowance while time remains. After exhaustion, changes remain locked until local midnight. Usage earlier today counts. Saving checks usage again, including when an editor was opened before exhaustion.
- **Schedule:** all schedule creation, app selection and timing controls. Enabled Normal and Strict schedules cannot be edited, disabled or deleted during their active window. A 9 PM?9 AM schedule locks at 9 PM and unlocks at 9 AM. Outside that window, changes are allowed. The storage layer rechecks this at save/delete time, and an open editor returns to the list when its saved schedule starts.
- **Settings:** permissions, protection options, DNS, diagnostics and strict provisioning.

These are in-app editing locks. Normal mode remains subject to Android permission revocation and force-stop. Daily allowances measure foreground usage, not continuous wall-clock countdowns.

### Payment compatibility and uninstall protection: unresolved requirements

This release does **not** claim payment compatibility is fixed. Paytm, Google Pay and Navi were reported to reject Still Focus's enabled Accessibility services. Google Pay [documents this category of restriction](https://support.google.com/pay/india/answer/17165523?hl=en-GB). Merely hiding our cover or changing navigation does not remove an enabled service from Android's permission state.

Regain's [privacy policy](https://regainapp.ai/legal/privacy-policy/) lists Accessibility, usage and overlay permissions. Its [website](https://regainapp.ai/) advertises uninstall prevention but does not document the implementation. We have not inspected the user's Regain permission state or verified its payment classification. Android's [app-access-risk verdict](https://developer.android.com/google/play/integrity/verdicts) can exempt Accessibility services that passed enhanced Google Play review; that is not proof that Regain has this exemption, that all three payment apps use that mechanism, or that publishing Still Focus alone will resolve the problem.

Still Focus retains its existing device-owner uninstall protection. Android's [setUninstallBlocked API](https://developer.android.com/reference/android/app/admin/DevicePolicyManager#setUninstallBlocked(android.content.ComponentName,java.lang.String,boolean)) requires privileged device-policy authority; ordinary usage/overlay permissions do not grant it. No new laptop-free uninstall lock was added. No payment mode, hidden permission restoration or payment-security bypass was added.

Required device validation: switch among all four tabs; add and exhaust a short daily allowance; attempt saving an already-open allowance editor after exhaustion; create a Normal overnight schedule and attempt edit/disable/delete inside and outside the active window; verify app/site timers and YouTube Shorts still enforce. Payment testing remains blocked by the reported permission rejection, and requires a separately reviewed compatibility/distribution approach rather than a UI change. No real transactions were performed during development.

## 0.25.0: Simpler navigation

Install [StillFocus-0.25.0.apk](artifacts/StillFocus-0.25.0.apk) over the existing app. This update keeps your rules and permissions.

- **Focus:** choose apps for a new pause, pause websites, or view running timers.
- **Blocks:** open Apps and YouTube Shorts, Websites, Daily limits or Schedules.
- **Apps:** choose applications; expand YouTube to select the whole app or Shorts only. Set hours/minutes and start.
- **Settings:** permissions, optional screen checks, DNS filtering and strict setup.

The shared theme uses charcoal surfaces and soft green controls. Payment mode is no longer offered as a new workflow. If it was already enabled in 0.24, use **Restore paused protection**, then manually restore your chosen Accessibility services in Android settings. Upgrading does not silently enable services or clear the existing pause.

**Payment compatibility is unresolved.** The user reports alerts in Paytm, Google Pay and Navi. Google Pay [documents restrictions on unknown apps with active Accessibility access](https://support.google.com/pay/india/answer/17165523?hl=en-GB). Regain's [official Play listing](https://play.google.com/store/apps/details?id=ai.regainapp) also declares Accessibility for Shorts blocking. This does not establish which services are enabled on the user's Regain installation or why payment apps accept it. This UI release does not change the permission architecture or claim to fix those alerts. Physical-device payment and blocking tests are still required; Play distribution alone is not a guarantee of compatibility.

The following 0.24 section describes the previous release; starting Payment mode is no longer offered in 0.25.

## 0.24.0: Payment mode and refreshed UI

Install [StillFocus-0.24.0.apk](artifacts/StillFocus-0.24.0.apk) over your current version:

```powershell
.\adb.exe install -r "D:\bulletproof-focus-android-app\artifacts\StillFocus-0.24.0.apk"
```

### Payment compatibility

A payment app may identify Still Focus's Accessibility services as enhanced access even when no overlay is visible. Combining the two services would not remove this capability. This release provides an explicit compatibility pause, not a way to conceal enabled permissions or bypass a bank's checks.

1. From Home or Settings, open **Payment mode > Start payment mode** and confirm the explained protection gap.
2. Still Focus persists the pause, immediately removes its normal/browser overlays, and calls `disableSelf()` on both connected Accessibility services. Usage-polling fallback is also paused so it cannot recreate a cover. If a service is enabled but disconnected, Android may still list it: use **Check Android Accessibility settings** and turn both Still Focus entries off. Wait for the page to report both off.
3. Open your payment app yourself and retry. Its security policy decides whether this resolves the alert; payment success has not been verified on the user's device. If it separately requests disabling display-over-apps permission, a shortcut opens that setting.
4. Return to Still Focus and tap **Finished paying - restore blocking**. Android Accessibility settings opens. Manually re-enable **Still Focus app blocking**, and **Still Focus screen-aware blocking** if you use Shorts/browser-page detection. Previously granted in-app consent is retained. Restore display-over-apps permission too if you turned it off.

Payment mode persists across process/service restarts and reboot. It does not automatically end, secretly re-enable Accessibility, disable Play Protect, or change another app's settings. Timers keep their original deadlines; daily usage continues counting while the normal controller runs, and saved daily exhaustion locks remain. **Normal app/daily-limit enforcement and native/web Shorts checks are inactive during the pause.** DNS filtering continues when the VPN is running. Existing device-owner suspensions and Strict restrictions remain active and are not relaxed by Payment mode. The Home screen and VPN notification identify the reduced protection.

Android requires the user to [enable Accessibility services in settings](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService). Google Pay documents [payment restrictions involving enabled Accessibility services](https://support.google.com/pay/india/answer/17165523?hl=en-GB); Paytm, Google Pay and Navi were subsequently reported affected; their handling of this specific APK remains unverified.

### Approved visual refresh

Ivory backgrounds, white cards, periwinkle controls and restrained teal accents replace the previous large coloured panels. In **Apps > Choose applications**, expand **YouTube** to choose **Whole application** or the nested **Shorts only** row. These are mutually exclusive choices within YouTube. Clear selection removes YouTube from the draft; existing active timers remain locked. Searching for Shorts also finds the YouTube parent. Other apps retain their icon/name checkboxes. The daily-limit and schedule pages share the updated palette.

Validation includes existing domain/usage/deadline tests, service-scope checks and a guard against payment mode clearing rules or restoring Accessibility/overlays. Build/lint and upgrade signing are checked. Physical-device checks remain necessary: payment-mode entry with both services on, enabled-but-disconnected services, VPN restart while paused, payment retry, restoring each service, daily-limit exhaustion during pause, and YouTube selection/search/rotation with large system text.

## 0.23.0: per-app daily limits and web Shorts pause requests

Install [StillFocus-0.23.0.apk](artifacts/StillFocus-0.23.0.apk) over the previous version:

```powershell
.\adb.exe install -r "D:\bulletproof-focus-android-app\artifacts\StillFocus-0.23.0.apk"
```

### Set daily limit

1. Open **Apps > Set daily limit**, enable Android **Usage access**, and keep normal app-blocking protection and display-over-other-apps access enabled.
2. Search for an app, tap it, and choose hours/minutes. Each app has its own daily allowance, from 1 minute to 24 hours.
3. The allowance counts total foreground use since local midnight, **including use earlier today**. Background audio is not counted. If today's use already exceeds the newly saved allowance, it locks immediately.
4. Edit or remove a limit while allowance remains. Once exhausted, both changes and removal are rejected by the store (including an editor left open across expiry). The app is intercepted using normal-mode app blocking with **Daily limit reached. Back at midnight.**
5. Allowances recur daily. Usage and exhaustion reset at the next local date; the configured limit remains. A backward date change does not reset saved counters. Each day's maximum observed usage and exhausted state persist across app/process restarts.

Daily limits are separate from schedules and pause timers; an app blocked by either still stays blocked. This first implementation uses **normal-mode interception**, even on a device enrolled for Strict mode; it does not add package suspension or uninstall protection for daily limits. Android Settings can stop protection. Usage access and a running blocker are required. Usage samples refresh in the background about every two seconds while protection is running; Android event delivery can add delay. Multi-window/OEM usage reporting, changes to the device clock/time zone and missing system history can affect accounting. Only configured apps' daily aggregate totals are saved locally; raw usage events are not stored or transmitted. Phone/calling apps and system navigation remain excluded.

### Web Shorts audio

When a detected YouTube Shorts web page is covered, the screen-aware service looks for explicitly labelled player Pause controls and requests a click. It never sends global media keys, changes volume or takes audio focus from other apps. The cover reports whether Pause was requested, the player exposes Play, or the control is unavailable. If the browser does not expose a supported control, audio can still continue; use the browser's media notification to pause it. Device verification is required. Native YouTube Shorts behavior remains unchanged.

The screen-aware disclosure now explains these player-control checks. Keep it enabled in Android Accessibility settings. No new permission, package scope or gesture capability is added. Browser address editing and normal tab controls remain available.

Validation: pure tests cover cumulative use across activities, screen-off/lock gaps, midnight clipping, independent apps, exact exhaustion and sticky locks; pause-label tests reject Play/unrelated controls. APK/lint and privacy guards are also run. Phone checks: set a small allowance, use the app in several visits, leave the editor open at exhaustion, restart Still Focus, try increasing/removing it, and verify next-day reset. Test Brave Nightly Shorts with sound, unavailable controls, a normal YouTube video, another music app, and calls.

Accounting follows Android's [UsageEvents lifecycle events](https://developer.android.com/reference/android/app/usage/UsageEvents.Event). Player actions use the existing [Accessibility node action mechanism](https://developer.android.com/guide/topics/ui/accessibility/service).

## 0.22.0: block the page, keep Brave open

Install [StillFocus-0.22.0.apk](artifacts/StillFocus-0.22.0.apk) over the previous version:

```powershell
.\adb.exe install -r "D:\bulletproof-focus-android-app\artifacts\StillFocus-0.22.0.apk"
```

The previous browser handler requested Home on blocked domains (and as a fallback for web Shorts). That could look like Brave closing repeatedly when reopening the same blocked tab; it is not evidence of a browser crash. Browser checks now cover only the exposed web-content area with **This page is blocked**. There is no browser Back/Home action, forced stop, automatic tab closure or history change. The address bar/tab controls stay available to navigate elsewhere. Native YouTube Shorts retains its existing Back/Home behavior.

Enable **Screen-aware blocking** and keep VPN protection on. If the service does not reconnect after updating, toggle this service off/on in Android Accessibility settings. Brave Stable/Beta/Nightly use the same bundled domain lists and custom domain/keyword rules as the VPN. No category name or blocked URL appears on the cover. Existing screen-access consent is retained; no extra package access or gesture capability is requested. The service now also locates the web-content rectangle and the focused application window; it skips HTML descendants and never reads page/form text.

The cover disappears on an allowed page, timer expiry, leaving the browser, service interruption or calls. Editing an address does not uncover an already blocked page; browser controls and the keyboard remain available. This is a visual/interaction block for detected pages, **not a guarantee that their network requests or background media never run**. DNS filtering remains the network layer. Unlisted domains, hidden URLs or missing web-content bounds can evade detection, so the app cannot promise to block every adult site. It does not intercept HTTPS content.

Device checks: keep a blocked tab plus an allowed tab open in Nightly; reopen the blocked tab repeatedly, type an allowed URL, switch tabs, open tab overview, rotate and try private tabs. Confirm the address bar, keyboard, system navigation and calls remain usable. Check both a custom timer and bundled rules, then timer expiry and service disable. APK/lint and host checks do not replace these physical-device checks.

Implementation uses Android's [accessibility overlay window type](https://developer.android.com/reference/android/view/accessibility/AccessibilityWindowInfo#TYPE_ACCESSIBILITY_OVERLAY), which permits inspecting the underlying app without navigating it away.

## 0.21.0: website quick picks and web Shorts timers

Install [StillFocus-0.21.0.apk](artifacts/StillFocus-0.21.0.apk) over your current version.

```powershell
.\adb.exe install -r "D:\bulletproof-focus-android-app\artifacts\StillFocus-0.21.0.apk"
```

1. Open **Websites**. Under **Quick picks**, tap YouTube, Shorts only, Instagram, Reddit, Facebook or X / Twitter. Tap again to deselect. You can still type custom domains.
2. **YouTube** blocks the full website. **Shorts only** creates a separate browser timer for `youtube.com/shorts` and its video paths, including mobile/www URLs. Picking either switches away from the other quick pick. Manually adding the whole YouTube domain also blocks regular videos.
3. For web Shorts, enable **Screen-aware blocking** below the selection, then choose hours/minutes and start. Active blocks show **YouTube Shorts (web)**. This timer is independent of native YouTube Shorts and other website timers. Pasting a Shorts URL also selects path-only blocking instead of silently blocking the whole domain.
4. Test in **Brave, Brave Beta or Brave Nightly**: open a normal YouTube watch page, then a `/shorts/VIDEO_ID` page. The Shorts page should request Back, with Home as a fallback, and show **Shorts are on a break**. Retry after expiry; Shorts should be accessible again.

Web Shorts uses the existing, explicitly enabled address-field Accessibility check, not DNS path filtering. It requires the browser to expose the current URL, including its path. Chrome is not in this screen-aware service's supported browser list. Hidden/elided addresses, private/fullscreen pages, or layouts that suppress the path can bypass this check; physical-device validation is still required. No arbitrary page content or browsing history is collected. Whole-site DNS blocking and native YouTube Shorts detection retain their existing behavior.

Host tests cover Shorts path boundaries, mobile URLs, encoded paths, ordinary watch pages, query/fragment false positives, lookalike hosts and timer expiry. For device testing also check selection after rotation, editing URLs, separate app/web timers, and service disable/re-enable.

## 0.20.0: Brave browser checks and YouTube Shorts timers

Install [StillFocus-0.20.0.apk](artifacts/StillFocus-0.20.0.apk) over the current version. In your platform-tools PowerShell window:

```powershell
.\adb.exe install -r "D:\bulletproof-focus-android-app\artifacts\StillFocus-0.20.0.apk"
```

### Enable the separate screen-aware service

Open **Settings and help > Screen-aware blocking > Set up screen-aware blocking**. Read the disclosure, agree, then enable **Still Focus screen-aware blocking** in Android Accessibility settings. Return to the app and confirm it is connected. Keep the original **Still Focus app blocking** service enabled for whole-app timers.

This is a new, separately consented service with window-content access restricted to YouTube and Brave Stable/Beta/Nightly. It checks supported browser address fields and YouTube resource identifiers/selected tab descriptions locally. Password fields are skipped; URLs and screen contents are not logged, stored or transmitted. This capability is broader than the original package-name-only service; the existing service retains its no-content-access configuration. Declared Accessibility capabilities can still trigger sideload warnings; the working USB development-install route remains available.

### Brave Nightly

Brave variants are explicitly retained in VPN DNS filtering even if discovered among calling providers. The optional screen-aware service additionally reads an exposed, unfocused address bar and returns Home when its hostname matches the same bundled/custom rules used by the DNS VPN. Both paths share one immutable blocklist index rather than loading separate copies. Matching pages may load briefly before detection; this is an access-interception layer, not HTTPS traffic filtering.

The user reported this bypass with browser Secure DNS already off; its exact cause has not been reproduced or confirmed on-device. Keep browser Secure DNS and Android Private DNS off for the DNS layer. Cached connections, direct IP/encrypted DNS, hidden address bars and private/fullscreen pages that suppress Accessibility can bypass one or both layers. The service does not read arbitrary page text, change browser settings, or guarantee classification of every site. Check **Settings > Keep protection running > Screen-aware blocking** for a generic status without revealing addresses.

### YouTube: Shorts only or whole application

1. Open **Apps > Choose applications**, select **YouTube**, then tap **Done**.
2. Under **What to block on YouTube?**, choose **Whole application** or **Shorts only**.
3. For Shorts only, choose **Normal** mode and enable the separate screen-aware service. Strict mode suspends an entire package and cannot selectively suspend Shorts.
4. Choose hours/minutes and start the timer. A detected Shorts screen requests Back with **Shorts are on a break**, falling back to Home if the screen persists; ordinary YouTube videos remain available unless covered by another whole-app schedule.
5. Active blocks show **YouTube Shorts** with its independent deadline. Older timers default to whole-app behavior on upgrade. Shorts options apply to manual YouTube timers, not YouTube Music, browser Shorts or recurring schedules.

Shorts detection is experimental: it uses specific player resource IDs or a selected Shorts tab (English/Hindi labels), not arbitrary video/page text or an unselected Shorts button. YouTube versions, locales, layout experiments, PiP and background playback can differ. There is no supported YouTube API for this UI distinction in this implementation; target-device testing is required. The service will not block all YouTube screens when it cannot identify Shorts.

### Validation

Host checks cover package boundaries, URL-to-host parsing, selected-versus-unselected Shorts controls, preview-shelf exclusion, separate timer flags and old-timer defaults, and the new service's narrow package allowlist/consent requirement. Existing DNS/domain and timer tests still run. Device checks remain pending: Brave Nightly normal/private tabs with a custom blocked domain; an allowed domain; editing the address bar; and YouTube Home/search/regular videos versus Shorts opened by tab or deep link. Check timer expiry, service disable/re-enable, rotation, calls, and background/Recents behavior. If YouTube markers are not exposed, diagnostics report that no supported Shorts marker was found; this is not proof that the screen is not Shorts.

## 0.19.0: scrollable duration wheels

Install [StillFocus-0.19.0.apk](artifacts/StillFocus-0.19.0.apk) over your existing version. Both Apps and Websites now have separate **Hours** and **Minutes** wheels. Scroll each column; the summary shows the combined duration (for example, 1 hr 30 min). Quick choices remain 2 min, 25 min and 1 hr. App and website drafts remain separate and survive navigation/rotation. A zero duration is rejected; the existing maximum of 365 days is retained (8,760 hours, 0 minutes). Active timers and schedules are unchanged.

Device check: select 1 hour 30 minutes, navigate away/back and rotate, verify the summary, and confirm the resulting timer is 90 minutes. Repeat on the website page; scrolling a wheel should not scroll the containing page. Check zero duration, quick choices and large system text. Physical-device visual verification is pending.

## 0.18.0: recovery and schedule fixes

Install [StillFocus-0.18.0.apk](artifacts/StillFocus-0.18.0.apk) over the current version and open Still Focus once to register the persisted recovery job. For USB installation from your platform-tools PowerShell folder:

```powershell
.\adb.exe install -r "D:\bulletproof-focus-android-app\artifacts\StillFocus-0.18.0.apk"
```

The reported Lava failure occurred with both Always-on VPN and Accessibility enabled. The VPN icon returned only after opening Still Focus. This indicates an interruption, but the exact process-stop cause has not yet been verified on-device.

Changes:

- Accessibility keeps the most recent foreground package during inactive windows, so a schedule can activate while the selected app remains open without a fresh window event.
- A persisted Android JobScheduler check attempts recovery periodically (15-minute minimum interval, subject to Android delays). It rearms schedule/timer alarms and requests the already-consented VPN service when disconnected. It does not override force-stop or revoked VPN consent.
- Accessibility connection/window events request missing VPN recovery, throttled to one attempt per minute. Alarm handling attempts service recovery even if policy reconciliation or alarm rearming fails.
- Settings diagnostics show last process-exit reason, recovery attempt, service heartbeat, next schedule boundary, last schedule alarm and precise-alarm access. No browsing/app-use history is recorded.
- Schedule cards distinguish an active time window from missing protection prerequisites.

After updating, test a short schedule that starts two minutes ahead while staying inside the selected app; then test the overnight 9 PM-9 AM schedule. Keep the existing Always-on VPN and Accessibility settings enabled, and confirm Alarms & reminders access for precise schedule boundaries. Leave Block connections without VPN off for this DNS-only VPN. Check the two-hour timer again without reopening Still Focus during the run. If protection stops, open Settings > Keep protection running and record the process-exit/recovery lines. Android's recorded exit reason is evidence, not a guarantee of the exact vendor-specific cause.

Host tests cover the retained foreground state, overnight boundaries and protected recovery service declaration. Build/lint do not replace long-running physical-device tests. Android may delay jobs during sleep or deny background restarts; the update does not guarantee continuous service under all power-management conditions. See [Android jobs](https://developer.android.com/reference/android/app/job/JobScheduler) and [Always-on VPN](https://developer.android.com/develop/connectivity/vpn).

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

Install [StillFocus-0.18.0.apk](artifacts/StillFocus-0.18.0.apk), then open **Still Focus**. Install over your existing version to retain settings, schedules and timers; do not uninstall first. This is a debug-signed testing build. If Android blocks installation, read the warning and see [Play Protect and distribution](#play-protect-and-distribution).

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

With explicit special-access permission, the VPN foreground service polls Android UsageEvents approximately every 200 ms to track the foreground package name in memory. It displays an application overlay only over user-selected apps during an active normal session. An optional, consented Accessibility service can instead receive app-window events and request the system Home action. The original app-blocking service retrieves no screen text or page contents. The separately enabled screen-aware service checks supported address fields and YouTube controls as described above; neither stores browsing/app-use history. System navigation and Settings remain accessible; the overlay does not appear over the lock screen.

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
