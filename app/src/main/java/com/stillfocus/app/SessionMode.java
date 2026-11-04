package com.stillfocus.app;

/** Strict requests must never silently become normal sessions. */
public final class SessionMode {
    private SessionMode() { }
    public static void validate(boolean strict,boolean deviceOwner,boolean appPermissions,boolean hasApps) {
        if(strict && !deviceOwner) throw new IllegalStateException("Strict mode requires Android 11+ and device-owner setup. Open Strict mode setup before starting. Root is not required.");
        if(!strict && hasApps && !appPermissions) throw new IllegalStateException("Enable Usage access and Display over other apps for normal app blocking.");
    }
}
