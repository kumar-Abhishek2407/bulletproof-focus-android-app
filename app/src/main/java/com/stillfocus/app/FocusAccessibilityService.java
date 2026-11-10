package com.stillfocus.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.view.accessibility.AccessibilityEvent;

/** Opt-in window-package events only; no screen content, nodes or gestures. */
public final class FocusAccessibilityService extends AccessibilityService {
    private static FocusAccessibilityService connected;
    private NormalAppBlocker blocker;
    public static boolean consented(Context c) {
        return c.getSharedPreferences("accessibilityConsent",0).getBoolean("accepted",false);
    }
    public static void consent(Context c,boolean accepted) {
        c.getSharedPreferences("accessibilityConsent",0).edit().putBoolean("accepted",accepted).apply();
    }
    public static boolean active() { return connected!=null && !PaymentMode.enabled(connected) && consented(connected); }
    public static boolean home() { return active() && connected.performGlobalAction(GLOBAL_ACTION_HOME); }
    public static void turnOff(Context c) {
        consent(c,false);
        if(connected!=null) { connected.stopBlocker(); connected.disableSelf(); connected=null; }
    }
    public static void pauseForPayment() {
        if(connected!=null) { connected.stopBlocker(); connected.disableSelf(); connected=null; }
    }
    @Override protected void onServiceConnected() {
        if(PaymentMode.enabled(this)) { disableSelf(); return; }
        if(!consented(this)) { disableSelf(); return; }
        stopBlocker(); connected=this;
        ProtectionRecovery.attempt(this,"Accessibility connection");
        blocker=new NormalAppBlocker(this,true); blocker.start();
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if(!active() || blocker==null || event==null || event.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        ProtectionRecovery.attempt(this,"Accessibility window event");
        CharSequence pkg=event.getPackageName();
        if(pkg!=null) blocker.windowChanged(pkg.toString());
    }
    private void stopBlocker() { if(blocker!=null) { blocker.stop(); blocker=null; } }
    @Override public void onInterrupt() { if(blocker!=null) blocker.windowChanged(""); }
    @Override public void onDestroy() { stopBlocker(); if(connected==this) connected=null; super.onDestroy(); }
}
