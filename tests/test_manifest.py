"""Guard the narrow, opt-in Accessibility capability and unrelated permissions."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
A = '{http://schemas.android.com/apk/res/android}'

class PermissionTests(unittest.TestCase):
    def test_filter_category_not_shown_in_ui(self):
        for name in ('MainActivity.java', 'ScheduleActivity.java', 'FocusVpnService.java'):
            source=(ROOT / 'app/src/main/java/com/stillfocus/app' / name).read_text(encoding='utf-8')
            source=source.replace('adult-domains.txt', '').replace('adult-exact.txt', '')
            self.assertNotIn('adult', source.lower(), name)

    def test_no_sensitive_sideload_services_or_permissions(self):
        manifest = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
        permissions = {e.get(A + 'name') for e in manifest.findall('uses-permission')}
        self.assertFalse(permissions & {'android.permission.READ_SMS', 'android.permission.RECEIVE_SMS',
            'android.permission.BIND_ACCESSIBILITY_SERVICE', 'android.permission.BIND_NOTIFICATION_LISTENER_SERVICE'})
        services = manifest.findall('application/service')
        self.assertEqual({s.get(A + 'name') for s in services}, {'.FocusVpnService', '.FocusAccessibilityService', '.ProtectionRecovery', '.FocusScreenService'})
        self.assertFalse((ROOT / 'app/src/main/java/com/stillfocus/app/AppBlockService.java').exists())
        self.assertFalse((ROOT / 'app/src/main/res/xml/accessibility.xml').exists())

if __name__ == '__main__':
    unittest.main()

class AccessibilityScopeTests(unittest.TestCase):
    def test_only_window_events_without_content_access(self):
        config=ET.parse(ROOT / 'app/src/main/res/xml/focus_accessibility.xml').getroot()
        self.assertEqual(config.get(A+'accessibilityEventTypes'), 'typeWindowStateChanged')
        self.assertEqual(config.get(A+'canRetrieveWindowContent'), 'false')
        self.assertEqual(config.get(A+'canPerformGestures'), 'false')
        self.assertEqual(config.get(A+'isAccessibilityTool'), 'false')
        manifest=ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
        service=next(s for s in manifest.findall('application/service') if s.get(A+'name')=='.FocusAccessibilityService')
        self.assertEqual(service.get(A+'permission'),'android.permission.BIND_ACCESSIBILITY_SERVICE')
        source=(ROOT / 'app/src/main/java/com/stillfocus/app/FocusAccessibilityService.java').read_text()
        for forbidden in ('getSource(', 'getRootInActiveWindow(', 'getText(', 'dispatchGesture('):
            self.assertNotIn(forbidden,source)
        self.assertIn('if(!consented(this)) { disableSelf(); return; }',source)

class RecoveryServiceTests(unittest.TestCase):
    def test_recovery_service_requires_system_job_permission(self):
        manifest=ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
        service=next(s for s in manifest.findall('application/service') if s.get(A+'name')=='.ProtectionRecovery')
        self.assertEqual(service.get(A+'permission'),'android.permission.BIND_JOB_SERVICE')
        vpn=next(s for s in manifest.findall('application/service') if s.get(A+'name')=='.FocusVpnService')
        self.assertEqual(vpn.get(A+'stopWithTask'),'false')

class ScreenServiceScopeTests(unittest.TestCase):
    def test_content_access_separate_and_package_restricted(self):
        config=ET.parse(ROOT / 'app/src/main/res/xml/screen_accessibility.xml').getroot()
        self.assertEqual(config.get(A+'canRetrieveWindowContent'),'true')
        self.assertEqual(config.get(A+'canPerformGestures'),'false')
        self.assertEqual(set(config.get(A+'packageNames').split(',')), {'com.google.android.youtube','com.brave.browser','com.brave.browser_beta','com.brave.browser_nightly'})
        manifest=ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
        service=next(s for s in manifest.findall('application/service') if s.get(A+'name')=='.FocusScreenService')
        self.assertEqual(service.get(A+'permission'),'android.permission.BIND_ACCESSIBILITY_SERVICE')
        source=(ROOT / 'app/src/main/java/com/stillfocus/app/FocusScreenService.java').read_text()
        self.assertIn('if(!consented(this)) { disableSelf(); return; }',source)
        self.assertNotIn('getText()',source[source.index('private boolean shortsVisible'):])

    def test_unknown_windows_are_rejected_before_their_root_is_requested(self):
        """Exercise the real gate methods with window metadata, without reading device screens."""
        source=(ROOT / 'app/src/main/java/com/stillfocus/app/FocusScreenService.java').read_text()
        self.assertNotIn('getRootInActiveWindow(',source)
        self.assertNotIn('event.getSource(',source)
        methods=source[source.index('    private boolean targetForeground('):source.index('    private void inspectBrowser(')]
        harness=r'''
package com.stillfocus.app;
import java.util.*;
public final class ScreenWindowProbe {
    String lastPackage="com.brave.browser_nightly";
    int lastWindowId=7;
    List<AccessibilityWindowInfo> windows=new ArrayList<>();
    boolean failWindows;
    List<AccessibilityWindowInfo> getWindows() {
        if(failWindows) throw new IllegalStateException("window metadata unavailable");
        return windows;
    }
    static final class AccessibilityNodeInfo {
        final int id; final String pkg; boolean recycled;
        AccessibilityNodeInfo(int id,String pkg) { this.id=id; this.pkg=pkg; }
        int getWindowId() { return id; }
        String getPackageName() { return pkg; }
        void recycle() { recycled=true; }
    }
    static final class AccessibilityWindowInfo {
        static final int TYPE_APPLICATION=1,TYPE_INPUT_METHOD=2,TYPE_SYSTEM=3,TYPE_ACCESSIBILITY_OVERLAY=4;
        final int id,type; final boolean focused; final AccessibilityNodeInfo root;
        int rootRequests; boolean recycled,failRoot;
        AccessibilityWindowInfo(int id,int type,boolean focused,String pkg) {
            this.id=id; this.type=type; this.focused=focused; root=new AccessibilityNodeInfo(id,pkg);
        }
        boolean isFocused() { return focused; }
        int getType() { return type; }
        int getId() { return id; }
        AccessibilityNodeInfo getRoot() {
            rootRequests++;
            if(failRoot) throw new IllegalStateException("target closed");
            return root;
        }
        void recycle() { recycled=true; }
    }
__METHODS__
    static AccessibilityWindowInfo window(int id,int type,boolean focused,String pkg) {
        return new AccessibilityWindowInfo(id,type,focused,pkg);
    }
    static ScreenWindowProbe gate(AccessibilityWindowInfo... windows) {
        ScreenWindowProbe result=new ScreenWindowProbe(); result.windows=Arrays.asList(windows); return result;
    }
    static void check(boolean okay,String message) { if(!okay) throw new AssertionError(message); }
    static void noRoots(ScreenWindowProbe gate,String message) {
        check(gate.foregroundRoot()==null,message);
        for(AccessibilityWindowInfo window:gate.windows) {
            check(window.rootRequests==0,message+": fetched a root");
            check(window.recycled,message+": leaked window metadata");
        }
    }
    public static void main(String[] args) {
        AccessibilityWindowInfo brave=window(7,1,true,"com.brave.browser_nightly");
        ScreenWindowProbe permitted=gate(brave);
        check(permitted.foregroundRoot()==brave.root && brave.rootRequests==1,"allow the confirmed browser window");
        check(permitted.targetForeground("com.brave.browser_nightly",7),"matching focus check");
        check(!permitted.targetForeground("com.google.android.apps.nbu.paisa.user",7),"reject a payment package");
        check(!permitted.targetForeground("com.brave.browser_nightly",8),"reject a stale window ID");
        check(brave.rootRequests==1,"focus-only checks must not request roots");
        noRoots(gate(window(7,1,false,"com.brave.browser_nightly"),window(9,1,true,"com.google.android.apps.nbu.paisa.user")),"payment foreground");
        noRoots(gate(window(7,1,true,"com.brave.browser_nightly"),window(9,3,true,"com.android.systemui")),"focused system dialog");
        noRoots(gate(window(7,1,false,"com.brave.browser_nightly")),"no focused app");
        noRoots(gate(window(10,1,true,"com.brave.browser_nightly")),"same package in an unconfirmed window");
        noRoots(gate(),"window metadata missing");
        ScreenWindowProbe invalid=gate(); invalid.lastWindowId=-1;
        check(invalid.foregroundRoot()==null,"no trusted event");
        invalid.lastWindowId=7; invalid.lastPackage="net.one97.paytm";
        check(invalid.foregroundRoot()==null,"package must be allowlisted");
        ScreenWindowProbe overlay=gate(window(7,1,true,"com.brave.browser_nightly"),window(8,4,false,"com.stillfocus.app"));
        check(overlay.foregroundRoot()!=null,"non-focusable cover must preserve page access");
        check(overlay.windows.get(1).rootRequests==0,"never fetch the cover root");
        ScreenWindowProbe keyboard=gate(window(7,1,true,"com.brave.browser_nightly"),window(8,2,false,"keyboard"));
        check(keyboard.foregroundRoot()!=null,"editing the address keeps the browser focused");
        check(keyboard.windows.get(1).rootRequests==0,"never fetch the keyboard root");
        AccessibilityWindowInfo changed=window(7,1,true,"com.dreamplug.androidapp");
        check(gate(changed).foregroundRoot()==null && changed.root.recycled,"reject a recycled/mismatched root before traversal");
        ScreenWindowProbe failed=gate(); failed.failWindows=true;
        check(failed.foregroundRoot()==null && !failed.focusedApplication(7),"metadata failure must deny access");
        AccessibilityWindowInfo closed=window(7,1,true,"com.brave.browser_nightly"); closed.failRoot=true;
        check(gate(closed).foregroundRoot()==null && closed.recycled,"window can close during a query");
        ScreenWindowProbe youtube=gate(window(11,1,true,ScreenRules.YOUTUBE)); youtube.lastWindowId=11; youtube.lastPackage=ScreenRules.YOUTUBE;
        check(youtube.foregroundRoot()!=null,"native Shorts still gets the confirmed YouTube root");
    }
}
'''.replace('__METHODS__',methods)
        javac=shutil.which('javac')
        java=shutil.which('java')
        self.assertIsNotNone(javac,'JDK is required for the screen-scope regression test')
        self.assertIsNotNone(java,'JDK is required for the screen-scope regression test')
        with tempfile.TemporaryDirectory(prefix='screen-window-scope-') as directory:
            probe=Path(directory) / 'ScreenWindowProbe.java'
            probe.write_text(harness,encoding='utf-8')
            base=ROOT / 'app/src/main/java/com/stillfocus/app'
            compiled=subprocess.run([javac,'-encoding','UTF-8','-d',directory,str(probe),str(base/'ScreenRules.java'),str(base/'DomainRules.java')],capture_output=True,text=True,timeout=30)
            self.assertEqual(compiled.returncode,0,compiled.stderr)
            checked=subprocess.run([java,'-cp',directory,'com.stillfocus.app.ScreenWindowProbe'],capture_output=True,text=True,timeout=30)
            self.assertEqual(checked.returncode,0,checked.stderr)

    def test_screen_actions_and_cover_recheck_the_confirmed_window(self):
        source=(ROOT / 'app/src/main/java/com/stillfocus/app/FocusScreenService.java').read_text()
        event=source[source.index('    @Override public void onAccessibilityEvent'):source.index('    private boolean queued')]
        self.assertLess(event.index('ScreenRules.browser(pkg)'),event.index('lastPackage=pkg'))
        self.assertLess(event.index('focusedApplication(eventWindowId)'),event.index('lastPackage=pkg'))
        browser=source[source.index('    private void inspectBrowser'):source.index('    private String pauseWebShorts')]
        self.assertIn('if(!targetForeground(pkg,root.getWindowId())) { hideBrowser(); return; }',browser)
        pause=source[source.index('    private String pauseWebShorts'):source.index('    private String playerLabel')]
        self.assertLess(pause.index('targetForeground(pkg,root.getWindowId())'),pause.index('performAction('))
        self.assertIn('node.getWindowId()!=root.getWindowId()',pause)
        cover=source[source.index('    private final Runnable coverFocusCheck'):source.index('    private void inspect()')]
        self.assertIn('!browserCover.visible()',cover)
        self.assertIn('targetForeground(lastPackage,lastWindowId)',cover)
        self.assertIn('hideBrowser(); return;',cover)
        self.assertIn('handler.postDelayed(this,100)',cover)
        self.assertIn('handler.removeCallbacks(coverFocusCheck)',source[source.index('    private void hideBrowser'):])

class BrowserCoverTests(unittest.TestCase):
    def test_browser_block_does_not_navigate_or_close_browser(self):
        source=(ROOT / 'app/src/main/java/com/stillfocus/app/FocusScreenService.java').read_text()
        browser=source[source.index('private void inspectBrowser'):source.index('private boolean shortsVisible')]
        self.assertNotIn('performGlobalAction',browser)
        self.assertNotIn('startActivity',browser)
        self.assertIn('WebsiteFilter.blocked(this,host)',browser)
        self.assertIn('browserCover.show(area,shorts,remaining)',browser)
        self.assertIn('if(!youtube) {\n                inspectBrowser(root,pkg,store);\n                shortsBackPending=false;\n                return;',source)
        overlay=(ROOT / 'app/src/main/java/com/stillfocus/app/BrowserPageCover.java').read_text()
        self.assertIn('TYPE_ACCESSIBILITY_OVERLAY',overlay)
        self.assertIn('FLAG_NOT_FOCUSABLE',overlay)
        self.assertIn('FLAG_NOT_TOUCH_MODAL',overlay)
        self.assertNotIn('GLOBAL_ACTION',overlay)
        self.assertNotIn('startActivity',overlay)

class DailyAndMediaTests(unittest.TestCase):
    def test_daily_limit_screen_is_private_and_uses_existing_blocker(self):
        manifest=ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
        activity=next(a for a in manifest.findall('application/activity') if a.get(A+'name')=='.DailyLimitActivity')
        self.assertEqual(activity.get(A+'exported'),'false')
        source=(ROOT / 'app/src/main/java/com/stillfocus/app/NormalAppBlocker.java').read_text()
        self.assertIn('paused.addAll(daily.blockedApps())',source)
        store=(ROOT / 'app/src/main/java/com/stillfocus/app/DailyLimitStore.java').read_text()
        self.assertIn('if(exhausted(pkg)) throw',store)
    def test_pause_targets_web_player_without_global_audio_controls(self):
        source=(ROOT / 'app/src/main/java/com/stillfocus/app/FocusScreenService.java').read_text()
        pause=source[source.index('private String pauseWebShorts'):source.index('private Rect browserPageBounds')]
        self.assertIn('webPageRoot(root)',pause)
        self.assertIn('ScreenRules.webShorts(current.value)',pause)
        self.assertIn('CallSafety.callAudio(this)',pause)
        for forbidden in ('dispatchMediaKeyEvent','requestAudioFocus','setStreamVolume','GLOBAL_ACTION'):
            self.assertNotIn(forbidden,pause)

class PaymentModeTests(unittest.TestCase):
    def test_payment_mode_disables_services_without_clearing_rules(self):
        base=ROOT / 'app/src/main/java/com/stillfocus/app'
        source=(base/'PaymentMode.java').read_text()
        self.assertLess(source.index('set(c,true)'),source.index('FocusAccessibilityService.pauseForPayment()'))
        for call in ('NormalAppBlocker.hideForPayment()', 'FocusAccessibilityService.pauseForPayment()', 'FocusScreenService.pauseForPayment()'):
            self.assertIn(call,source)
        for forbidden in ('FocusStore', 'ScheduleStore', 'DailyLimitStore', 'Settings.Secure', 'startService', '.clear()'):
            self.assertNotIn(forbidden,source)
        for name in ('FocusAccessibilityService.java','FocusScreenService.java'):
            service=(base/name).read_text()
            self.assertIn('if(PaymentMode.enabled(this)) { disableSelf(); return; }',service)
            self.assertIn('!PaymentMode.enabled(connected)',service)
        blocker=(base/'NormalAppBlocker.java').read_text()
        self.assertLess(blocker.index('if(PaymentMode.enabled(context))'),blocker.index('if(!eventDriven && FocusAccessibilityService.active())'))
        self.assertIn('daily.requestRefresh(); hide(); foreground.clear(); exit.clear(); since=0; return;',blocker)
