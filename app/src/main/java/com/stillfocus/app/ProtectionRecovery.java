package com.stillfocus.app;

import android.app.*;
import android.app.job.*;
import android.content.*;
import android.net.VpnService;
import android.os.Build;
import android.os.SystemClock;

/** Best-effort recovery using Android jobs; never bypasses revoked VPN consent or force-stop. */
public final class ProtectionRecovery extends JobService {
    private static final int JOB=18;
    private static long lastAttempt;
    public static void record(Context c,String message) {
        c.getSharedPreferences("protectionHealth",0).edit().putString("recovery",message)
            .putLong("recoveryAt",System.currentTimeMillis()).apply();
    }
    public static void schedule(Context c) {
        if(!new FocusStore(c).enabled()) return;
        JobScheduler jobs=c.getSystemService(JobScheduler.class);
        if(jobs.getPendingJob(JOB)!=null) return;
        int result=jobs.schedule(new JobInfo.Builder(JOB,new ComponentName(c,ProtectionRecovery.class))
            .setPersisted(true).setPeriodic(15*60*1000L).build());
        if(result!=JobScheduler.RESULT_SUCCESS) record(c,"Android could not schedule the recovery check");
    }
    public static synchronized void attempt(Context c,String source) {
        if(!new FocusStore(c).enabled() || FocusVpnService.running) return;
        long now=SystemClock.elapsedRealtime();
        if(lastAttempt!=0 && now-lastAttempt<60000) return;
        lastAttempt=now;
        if(VpnService.prepare(c)!=null) { record(c,"VPN permission missing; recovery requires consent"); return; }
        try {
            c.startForegroundService(new Intent(c,FocusVpnService.class));
            record(c,"Restart requested by "+source);
        } catch(RuntimeException denied) { record(c,"Restart denied by Android ("+denied.getClass().getSimpleName()+"). Open Still Focus."); }
    }
    @Override public boolean onStartJob(JobParameters params) {
        try { ManagedProtection.reconcile(this); } catch(RuntimeException e) { record(this,"Policy recovery needs attention"); }
        try { ScheduleReceiver.schedule(this); } catch(RuntimeException e) { record(this,"Schedule alarm could not be rearmed"); }
        try { SessionReceiver.schedule(this); } catch(RuntimeException e) { record(this,"Timer alarm could not be rearmed"); }
        attempt(this,"periodic recovery check");
        return false;
    }
    @Override public boolean onStopJob(JobParameters params) { return true; }
    public static String lastExit(Context c) {
        if(Build.VERSION.SDK_INT<30) return "";
        try {
            java.util.List<ApplicationExitInfo> exits=c.getSystemService(ActivityManager.class).getHistoricalProcessExitReasons(c.getPackageName(),0,1);
            if(exits.isEmpty()) return "";
            ApplicationExitInfo e=exits.get(0);
            String reason;
            switch(e.getReason()) {
                case ApplicationExitInfo.REASON_LOW_MEMORY: reason="low memory"; break;
                case ApplicationExitInfo.REASON_CRASH: case ApplicationExitInfo.REASON_CRASH_NATIVE: reason="crash"; break;
                case ApplicationExitInfo.REASON_ANR: reason="app not responding"; break;
                case ApplicationExitInfo.REASON_USER_REQUESTED: reason="user/system stop request"; break;
                case ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE: reason="resource use"; break;
                default: reason="Android reason "+e.getReason();
            }
            return "\nLast process exit: "+android.text.format.DateFormat.format("MMM d, HH:mm",e.getTimestamp())+" - "+reason;
        } catch(RuntimeException unavailable) { return "\nProcess-exit details unavailable"; }
    }
}
