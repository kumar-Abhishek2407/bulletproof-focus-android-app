package com.stillfocus.app;
import android.content.*;
import android.app.usage.*;
import android.os.SystemClock;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Recurring normal-mode limits, with durable exhaustion locks until local midnight. */
public final class DailyLimitStore {
    private final Context context;
    private final SharedPreferences prefs;
    private static final Object LOCK=new Object();
    private static final ExecutorService worker=Executors.newSingleThreadExecutor();
    private static long lastRefresh;
    private static boolean pending;
    public static volatile String status="Usage access required";
    public DailyLimitStore(Context c) { context=c.getApplicationContext(); prefs=context.getSharedPreferences("dailyLimits",0); }
    private void rollDay() {
        long today=LocalDate.now().toEpochDay(),saved=prefs.getLong("day",Long.MIN_VALUE);
        if(today>saved) {
            SharedPreferences.Editor edit=prefs.edit();
            for(String key:prefs.getAll().keySet()) if(key.startsWith("used.") || key.startsWith("locked.")) edit.remove(key);
            if(!edit.putLong("day",today).commit()) throw new IllegalStateException("Could not reset daily usage.");
        }
    }
    public Map<String,Integer> limits() {
        synchronized(LOCK) { rollDay(); Map<String,Integer> result=new HashMap<>();
            for(Map.Entry<String,?> e:prefs.getAll().entrySet()) if(e.getKey().startsWith("limit.")) result.put(e.getKey().substring(6),(Integer)e.getValue()); return result;
        }
    }
    public long used(String pkg) { synchronized(LOCK) { rollDay(); return prefs.getLong("used."+pkg,0); } }
    public boolean exhausted(String pkg) { synchronized(LOCK) { rollDay(); int limit=prefs.getInt("limit."+pkg,0); return limit>0 && DailyUsageCounter.exhausted(prefs.getLong("used."+pkg,0),limit,prefs.getBoolean("locked."+pkg,false)); } }
    public Set<String> blockedApps() {
        requestRefresh(); Set<String> result=new HashSet<>(); for(String pkg:limits().keySet()) if(exhausted(pkg)) result.add(pkg); return result;
    }
    public void requestRefresh() {
        synchronized(LOCK) {
            if(pending || SystemClock.elapsedRealtime()-lastRefresh<2000 || limits().isEmpty()) return;
            pending=true;
        }
        worker.execute(()-> { try { refresh(); } catch(RuntimeException e) { status="Usage check unavailable; reopen Still Focus"; }
            finally { synchronized(LOCK) { pending=false; lastRefresh=SystemClock.elapsedRealtime(); } }
        });
    }
    private Map<String,Long> query() {
        if(!NormalAppBlocker.usageAllowed(context)) throw new IllegalStateException("Enable Usage access to measure daily use.");
        long now=System.currentTimeMillis(),start=LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        UsageEvents events=context.getSystemService(UsageStatsManager.class).queryEvents(start-86400000L,now);
        if(events==null) throw new IllegalStateException("Android usage history is unavailable. Unlock your phone and retry.");
        DailyUsageCounter counter=new DailyUsageCounter(start,now); UsageEvents.Event event=new UsageEvents.Event();
        while(events.hasNextEvent()) {
            events.getNextEvent(event); long at=event.getTimeStamp();
            switch(event.getEventType()) {
                case UsageEvents.Event.ACTIVITY_RESUMED: counter.resume(at,event.getPackageName(),event.getClassName()); break;
                case UsageEvents.Event.ACTIVITY_PAUSED: counter.pause(at,event.getPackageName(),event.getClassName()); break;
                case UsageEvents.Event.SCREEN_INTERACTIVE: counter.screen(at,true); break;
                case UsageEvents.Event.SCREEN_NON_INTERACTIVE: counter.screen(at,false); break;
                case UsageEvents.Event.KEYGUARD_SHOWN: counter.lock(at,true); break;
                case UsageEvents.Event.KEYGUARD_HIDDEN: counter.lock(at,false); break;
                case UsageEvents.Event.DEVICE_SHUTDOWN:
                case UsageEvents.Event.DEVICE_STARTUP: counter.stop(at); break;
            }
        }
        return counter.finish();
    }
    private void merge(Map<String,Long> measured) {
        rollDay(); SharedPreferences.Editor edit=prefs.edit();
        for(Map.Entry<String,Integer> entry:limits().entrySet()) {
            String pkg=entry.getKey(); long used=Math.max(prefs.getLong("used."+pkg,0),measured.getOrDefault(pkg,0L));
            edit.putLong("used."+pkg,used);
            if(DailyUsageCounter.exhausted(used,entry.getValue(),prefs.getBoolean("locked."+pkg,false))) edit.putBoolean("locked."+pkg,true);
        }
        if(!edit.commit()) throw new IllegalStateException("Could not save daily usage.");
        status="Usage checked; limits reset at midnight";
    }
    private void refresh() {
        long day=LocalDate.now().toEpochDay(); Map<String,Long> measured=query();
        synchronized(LOCK) { if(day==LocalDate.now().toEpochDay()) merge(measured); }
    }
    public void save(String pkg,int minutes) {
        if(minutes<0 || minutes>1440) throw new IllegalArgumentException("Choose 1 minute to 24 hours.");
        if(CallSafety.protectedApp(context,pkg) || context.getPackageName().equals(pkg) || "com.android.settings".equals(pkg)) throw new IllegalArgumentException("This app cannot have a daily limit.");
        if(!NormalAppBlocker.ready(context)) throw new IllegalStateException("Enable app blocking and display-over-other-apps access first.");
        if(!FocusAccessibilityService.active() && !FocusVpnService.running) throw new IllegalStateException("Open Still Focus and restore protection before setting a daily limit.");
        long day=LocalDate.now().toEpochDay(); Map<String,Long> measured=query();
        synchronized(LOCK) {
            if(day!=LocalDate.now().toEpochDay()) throw new IllegalStateException("A new day started. Please try again.");
            merge(measured);
            if(exhausted(pkg)) throw new IllegalStateException("Daily limit reached. Changes unlock at midnight.");
            long used=Math.max(prefs.getLong("used."+pkg,0),measured.getOrDefault(pkg,0L));
            SharedPreferences.Editor edit=prefs.edit().putLong("used."+pkg,used);
            if(minutes==0) edit.remove("limit."+pkg);
            else edit.putInt("limit."+pkg,minutes).putBoolean("locked."+pkg,DailyUsageCounter.exhausted(used,minutes,false));
            if(!edit.commit()) throw new IllegalStateException("Could not save daily limit.");
        }
    }
    public long untilReset(String pkg) { if(!exhausted(pkg)) return 0; ZonedDateTime now=ZonedDateTime.now(); return Math.max(1,Duration.between(now,now.toLocalDate().plusDays(1).atStartOfDay(now.getZone())).toMillis()); }
}
