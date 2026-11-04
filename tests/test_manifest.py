"""Guard the narrow, opt-in Accessibility capability and unrelated permissions."""
from pathlib import Path
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
        self.assertEqual({s.get(A + 'name') for s in services}, {'.FocusVpnService', '.FocusAccessibilityService'})
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
