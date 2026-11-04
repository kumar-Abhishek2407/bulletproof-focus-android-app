package com.stillfocus.app;
import android.content.*;
import android.net.VpnService;
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent) {
        if(!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) return;
        // A policy/alarm error must never prevent consented network protection recovery.
        try { ManagedProtection.reconcile(c); } catch(RuntimeException ignored) { }
        try { SessionReceiver.schedule(c); } catch(RuntimeException ignored) { }
        try { ScheduleReceiver.schedule(c); } catch(RuntimeException ignored) { }
        if(new FocusStore(c).enabled() && VpnService.prepare(c)==null) {
            try { c.startForegroundService(new Intent(c,FocusVpnService.class)); }
            catch(RuntimeException e) { FocusVpnService.status="Open Still Focus to resume website protection"; }
        }
    }
}
