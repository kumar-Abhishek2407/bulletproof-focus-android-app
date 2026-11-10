# Payment compatibility investigation

Updated 10 November 2026. Affected apps reported by the user: Google Pay, Paytm and Navi on Lava Curve 5G / Android 16.

## What is established

The reported warning names Still Focus and asks the user to disable its Accessibility permission. Both of our services were reported to trigger the warning. "Screen-aware blocking" is the name of a second Accessibility service, not a MediaProjection/screen-recording permission.

Google Pay says it can reject unknown or unauthorised apps with enabled Accessibility services, and explicitly mentions non-Play installations: https://support.google.com/pay/india/answer/17165523?hl=en-GB

Google's app-access-risk verdict distinguishes known/unknown installed, controlling and overlay apps. It documents exceptions for verified Accessibility services: https://developer.android.com/google/play/integrity/verdicts

Regain declares Accessibility in its Play listing and privacy policy:
- https://play.google.com/store/apps/details?id=ai.regainapp
- https://regainapp.ai/legal/privacy-policy/

Those facts establish a possible trust/distribution difference. They do not prove Regain is exempt, identify the actual security SDK used by Paytm or Navi, or guarantee a Play-installed Still Focus will be accepted. Setting `isAccessibilityTool=true` would misrepresent this general focus app; it is not a compatibility fix.

## Device evidence

Earlier read-only inspection found Still Focus 0.26.0 installed through ADB, debuggable, with usage and overlay access enabled and both Accessibility services off. Regain was absent. The latest inspection again found Regain absent and also reported Still Focus absent. No payment screen, PIN, transaction history or authentication data was inspected. No payment transaction was attempted. There is no current side-by-side Regain trace.

Run `python tools/inspect-blocker-permissions.py` with a connected authorised device to repeat the package/permission comparison. A missing app means comparison is unavailable, not compatible.

## Alternatives and their limits

| Approach | Preserves | Limitation |
| --- | --- | --- |
| Usage access and app overlay, with both Accessibility services off | Whole-app timers, daily limits, schedules, DNS filtering | Detection can lag. Does not identify native Shorts or inspect Brave's current page. Some payment apps may also reject overlay permission. |
| Existing scoped screen service | Native YouTube Shorts and supported Brave page checks | Still an enabled Accessibility service, so permission-based bank checks may reject it. |
| DNS filtering alone | Domain filtering and DNS ad blocking | Cannot reliably distinguish Shorts from ordinary YouTube videos sharing the same domains; URLs inside HTTPS are unavailable to DNS. |
| A browser controlled by Still Focus | Could inspect its own navigation without Accessibility | Would require a separate browsing experience; would not control native YouTube or existing Brave tabs. Not implemented. |
| Managed Android app suspension | OS-enforced whole-app pauses | Requires provisioning; does not distinguish Shorts from ordinary videos. |

MediaProjection/screen capture is not a suitable replacement for resolving this warning: it introduces a different sensitive capability. Hiding an overlay, renaming the app, spoofing installation provenance or mislabelling Accessibility capabilities does not establish safe compatibility.

## 0.28 changes

- Screen inspection is gated by a target window ID from an already allowlisted Accessibility event. An unrelated focused window is rejected before requesting its content, including the final recheck before a native Shorts navigation action.
- A visible browser cover gets a short-lived 100 ms window-metadata focus check so it is removed after switching away; no unrelated screen roots are read by that check. Android scheduling means this is not an absolute deadline.
- Device-test builds are non-debuggable. They deliberately retain the existing development signing certificate so they can upgrade prior APKs without deleting user data. This is not production signing, Play review, or a guarantee of payment acceptance.
- No Accessibility feature is removed or automatically toggled; no payment-specific pause mode is added.

Build with `powershell -NoProfile -ExecutionPolicy Bypass -File tools/build.ps1 -DeviceBuild -Lint`. The original debug build remains available by omitting `-DeviceBuild`.

## Remaining validation

Test the new APK with the same explicitly enabled services and payment-app versions. First check that the prior security alert clears; a successful app launch alone is not proof of successful payment. The user must control any authentication or transaction. If the enabled-permission warning remains, collect the APK hash/certificate, version, installation source and exact warning for the payment provider and any applicable Play review/appeal. A reviewed distribution path and provider investigation can address classification; local UI changes cannot guarantee an external provider's decision.
