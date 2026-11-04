package com.stillfocus.app;
import android.app.*;
import android.content.*;
import android.os.SystemClock;
public final class SessionReceiver extends BroadcastReceiver {
    public static void schedule(Context c) {
        long remaining=new FocusStore(c).nextExpiry();
        if(remaining==0) return;
        PendingIntent callback=PendingIntent.getBroadcast(c,4,new Intent(c,SessionReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        c.getSystemService(AlarmManager.class).setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime()+remaining,callback);
    }
    @Override public void onReceive(Context c,Intent i) { ManagedProtection.reconcile(c); schedule(c); }
}
