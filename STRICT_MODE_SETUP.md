# Strict mode setup (Still Focus 0.7.0)

Strict mode requires Android 11+ and device-owner provisioning. **Root is not required.** An ordinary APK install, Accessibility grant or legacy device-admin activation does not enable uninstall protection.

## Before provisioning

Use a dedicated test phone or emulator where possible. Back up a personal phone first: Android's provisioning requirements may require a factory reset. No command in this guide resets a phone, removes accounts or bypasses Android's checks, and the app does not perform those actions.

The phone must have no existing device owner, managed profile or accounts that prevent provisioning. Exact eligibility depends on Android and the manufacturer. If provisioning is refused, read the error before taking further action; do not remove accounts or reset the phone without understanding the data impact.

## Provision from your computer

1. Install `artifacts/StillFocus-0.7.0.apk` on the phone.
2. Install [Android SDK Platform Tools](https://developer.android.com/tools/releases/platform-tools) on your computer.
3. On your own phone, enable Developer options and USB debugging. Connect over USB and approve your computer on the phone.
4. In the platform-tools folder, run `adb devices` (PowerShell may need `.\adb.exe devices`). Confirm the intended phone is listed and authorized. If multiple devices appear, use `adb -s SERIAL` for every command below.
5. Run this provisioning command against the intended phone:

```text
adb shell dpm set-device-owner com.stillfocus.app/.FocusAdminReceiver
```

The app also exposes this command on **Settings and help > Strict mode setup** with a Copy button. This app does not execute provisioning automatically. Leave Play Protect enabled; Google may require DPC review/approval during enrollment. If blocked, follow [Google's developer guidance](https://developers.google.com/android/play-protect/warning-dev-guidance).

## Use and verify

Reopen Still Focus and choose **Strict** under Protection mode on the app or website blocking page. Confirm **Device owner: ready**. Select a nonessential app or test website, choose a short duration such as two minutes, and confirm the strict-session disclosure.

Strict mode does not need Usage access or an overlay for app suspension. VPN consent and the running protection service are still required by this build to filter websites and reconcile timer policies. The one-time general permission setup remains available for normal-mode features.

Before confirming a successful strict session, the app checks that Android reports uninstall blocking and disabled user controls for Still Focus. If setup is absent or a requested policy/suspension fails, it rejects session creation and attempts to restore partial policies; it does not silently fall back to normal mode.

On a dedicated test phone, confirm that uninstall, force-stop and data clearing for Still Focus are unavailable in Settings while the timer runs. Selected apps should show Android's suspended-app behavior. Confirm there is no in-app cancel, mode change or device-owner removal during the strict timer. Restart/unlock and verify that the same timer/mode is recovered.

At expiry, the app restores the policies and app-suspension state it changed. An inexact alarm and system sleep can delay release; reopening Still Focus reconciles immediately. **Device-owner enrollment remains after expiry.** To uninstall, open **Settings and help > Remove device-owner setup** after the strict timer ends, confirm removal, then uninstall normally. Removing enrollment does not reset the phone. A future strict session will require provisioning again.

## Scope and limits

The timer uses monotonic time within a boot and a persisted calendar deadline across boots. Strict sessions restrict date/time configuration. A user with root, privileged debugging or reset capabilities remains outside this guarantee. This implementation does not disable USB debugging or prevent factory resets.

Strict mode protects Still Focus's uninstall/data controls and suspends selected apps. It is not a full network lockdown. Browser Secure DNS, proxies, direct IP connections and DNS caching can still bypass website rules. Do not enable VPN lockdown / Block connections without VPN with this DNS-only VPN. Normal mode remains available with its weaker, voluntary protections.

App suspension, uninstall policies, rollback and reboot behavior must be verified on each target phone; desktop tests and APK compilation cannot prove Android enforcement.

[Android DevicePolicyManager reference](https://developer.android.com/reference/android/app/admin/DevicePolicyManager)

## Recurring schedules

Strict schedules use the same device-owner setup. During an active window they prevent uninstall and suspend their chosen apps. Overlapping strict timers/schedules share the policy journal; apps are restored only after all strict blocks covering them end. Active strict schedules cannot be edited or disabled. Disable future strict schedules outside their active windows before removing device-owner enrollment. The app requires a daily gap between strict schedules to preserve a time when this is possible.
