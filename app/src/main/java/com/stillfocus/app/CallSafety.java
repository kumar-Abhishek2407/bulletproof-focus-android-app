package com.stillfocus.app;

import android.content.*;
import android.content.pm.*;
import android.media.AudioManager;
import android.telecom.TelecomManager;
import java.util.*;

public final class CallSafety {
    private static Set<String> cached;
    private CallSafety() { }
    public static synchronized void refresh() { cached=null; }
    public static synchronized Set<String> packages(Context c) {
        if(cached==null) {
            Set<String> result=new HashSet<>(Arrays.asList("com.truecaller","com.android.phone","com.android.server.telecom","com.android.ims","com.mediatek.ims","com.qualcomm.qti.ims","com.google.android.dialer","com.android.dialer","com.whatsapp","com.whatsapp.w4b","org.thoughtcrime.securesms","org.telegram.messenger","us.zoom.videomeetings","com.microsoft.teams","com.google.android.apps.tachyon"));
            try { String dialer=c.getSystemService(TelecomManager.class).getDefaultDialerPackage(); if(dialer!=null) result.add(dialer); } catch(RuntimeException ignored) { }
            for(ResolveInfo info:c.getPackageManager().queryIntentServices(new Intent("android.telecom.ConnectionService"),0)) if(info.serviceInfo!=null) result.add(info.serviceInfo.packageName);
            cached=result;
        }
        return new HashSet<>(cached);
    }
    public static boolean protectedApp(Context c,String pkg) { return packages(c).contains(pkg) || pkg.contains("dialer") || pkg.contains("emergency"); }
    public static boolean callAudio(Context c) {
        try { return c.getSystemService(AudioManager.class).getMode()!=AudioManager.MODE_NORMAL; }
        catch(RuntimeException ignored) { return true; }
    }
}
