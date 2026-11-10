package com.stillfocus.app;

import android.content.Context;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityManager;

/** Explicit compatibility pause. Never conceals permissions or reenables Accessibility. */
public final class PaymentMode {
    private PaymentMode() { }
    public static boolean enabled(Context c) { return c.getSharedPreferences("paymentMode",0).getBoolean("enabled",false); }
    public static void begin(Context c) {
        set(c,true); // Persist before disabling: VPN recovery/reboots must not restore overlays.
        NormalAppBlocker.hideForPayment();
        FocusAccessibilityService.pauseForPayment();
        FocusScreenService.pauseForPayment();
    }
    public static void finish(Context c) { set(c,false); }
    private static void set(Context c,boolean enabled) {
        if(!c.getSharedPreferences("paymentMode",0).edit().putBoolean("enabled",enabled).commit()) throw new IllegalStateException("Could not save payment mode.");
    }
    public static int enabledServices(Context c) {
        int count=0;
        for(AccessibilityServiceInfo info:c.getSystemService(AccessibilityManager.class).getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            android.content.pm.ResolveInfo resolved=info.getResolveInfo();
            if(resolved!=null && resolved.serviceInfo!=null && c.getPackageName().equals(resolved.serviceInfo.packageName)) count++;
        }
        return count;
    }
}
