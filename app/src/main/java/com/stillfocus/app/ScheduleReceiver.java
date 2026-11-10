package com.stillfocus.app;

import android.app.*;
import android.content.*;
import android.net.VpnService;
import android.os.Build;
import java.time.ZonedDateTime;

public final class ScheduleReceiver extends BroadcastReceiver {
    public static boolean exactAllowed(Context c) { return Build.VERSION.SDK_INT<31 || c.getSystemService(AlarmManager.class).canScheduleExactAlarms(); }
    public static void schedule(Context c) {
        ZonedDateTime now=ScheduleStore.now(); long next=Long.MAX_VALUE;
        for(AppSchedule s:new ScheduleStore(c).all()) if(s.enabled) next=Math.min(next,s.window.nextBoundary(now));
        AlarmManager alarms=c.getSystemService(AlarmManager.class);
        PendingIntent callback=PendingIntent.getBroadcast(c,8,new Intent(c,ScheduleReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        c.getSharedPreferences("protectionHealth",0).edit().putLong("nextSchedule",next==Long.MAX_VALUE?0:next).apply();
        if(next==Long.MAX_VALUE) { alarms.cancel(callback); return; }
        if(exactAllowed(c)) {
            try { alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,callback); return; }
            catch(SecurityException revoked) { /* Fall back if special access changed in flight. */ }
        }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,callback);
    }
    @Override public void onReceive(Context c,Intent i) {
        c.getSharedPreferences("protectionHealth",0).edit().putLong("lastScheduleAlarm",System.currentTimeMillis()).apply();
        try { ManagedProtection.reconcile(c); }
        catch(RuntimeException e) { ProtectionRecovery.record(c,"Schedule policy needs attention"); }
        // Alarm failures must not skip the independent service-recovery attempt.
        try { schedule(c); } catch(RuntimeException e) { ProtectionRecovery.record(c,"Schedule alarm could not be rearmed"); }
        try { ProtectionRecovery.schedule(c); } catch(RuntimeException ignored) { }
        ProtectionRecovery.attempt(c,"schedule boundary");
    }
}
