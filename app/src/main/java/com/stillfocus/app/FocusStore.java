package com.stillfocus.app;

import android.content.*;
import android.os.SystemClock;
import android.provider.Settings;
import org.json.*;
import java.util.*;

/** Independent, persistent deadlines per app/domain. Legacy sessions migrate intact. */
public final class FocusStore {
    private final Context context;
    private final android.content.SharedPreferences prefs;
    private static String cachedRaw;
    private static List<TimedBlock> cached;
    public FocusStore(Context c) { context=c.getApplicationContext(); prefs=context.getSharedPreferences("focus",0); }
    private int boot() { return Settings.Global.getInt(context.getContentResolver(),Settings.Global.BOOT_COUNT,-1); }
    private List<TimedBlock> read() {
        synchronized(FocusStore.class) {
            if(!prefs.contains("timers")) {
                List<TimedBlock> migrated=new ArrayList<>();
                long wall=prefs.getLong("end",0),elapsed=prefs.getLong("elapsedEnd",0); int savedBoot=prefs.getInt("boot",-2); boolean strict=prefs.getBoolean("strictSession",true);
                if(SessionDeadline.remaining(wall,elapsed,savedBoot,boot(),System.currentTimeMillis(),SystemClock.elapsedRealtime())>0) {
                    for(String target:prefs.getStringSet("apps",Collections.emptySet())) migrated.add(new TimedBlock("legacy-app-"+target,target,true,strict,wall,elapsed,savedBoot));
                    for(String target:prefs.getStringSet("domains",Collections.emptySet())) migrated.add(new TimedBlock("legacy-site-"+target,target,false,strict,wall,elapsed,savedBoot));
                }
                write(migrated);
            }
            String raw=prefs.getString("timers","[]");
            if(!raw.equals(cachedRaw)) {
                List<TimedBlock> list=new ArrayList<>();
                try {
                    JSONArray values=new JSONArray(raw);
                    for(int i=0;i<values.length();i++) {
                        JSONObject j=values.getJSONObject(i);
                        list.add(new TimedBlock(j.getString("id"),j.getString("target"),j.getBoolean("app"),j.getBoolean("strict"),j.getLong("wallEnd"),j.getLong("elapsedEnd"),j.getInt("boot"),j.optBoolean("shorts",false)));
                    }
                } catch(JSONException e) { throw new IllegalStateException("Could not read timers",e); }
                cachedRaw=raw; cached=list;
            }
            return new ArrayList<>(cached);
        }
    }
    private void write(List<TimedBlock> timers) {
        JSONArray values=new JSONArray();
        try {
            for(TimedBlock t:timers) {
                JSONObject j=new JSONObject(); j.put("id",t.id); j.put("target",t.target); j.put("app",t.app); j.put("strict",t.strict); j.put("shorts",t.shorts); j.put("wallEnd",t.wallEnd); j.put("elapsedEnd",t.elapsedEnd); j.put("boot",t.boot); values.put(j);
            }
        } catch(JSONException e) { throw new IllegalStateException(e); }
        if(!prefs.edit().putString("timers",values.toString()).commit()) throw new IllegalStateException("Could not save timers");
    }
    public long remaining(TimedBlock t) { return t.remaining(boot(),System.currentTimeMillis(),SystemClock.elapsedRealtime()); }
    public List<TimedBlock> active() {
        List<TimedBlock> active=new ArrayList<>(); int currentBoot=boot(); long wall=System.currentTimeMillis(),elapsed=SystemClock.elapsedRealtime();
        for(TimedBlock t:read()) if(t.remaining(currentBoot,wall,elapsed)>0) active.add(t);
        active.sort(Comparator.comparingLong(this::remaining)); return active;
    }
    public long remaining() { long max=0; for(TimedBlock t:active()) max=Math.max(max,remaining(t)); return max; }
    public long nextExpiry() { long min=Long.MAX_VALUE; for(TimedBlock t:active()) min=Math.min(min,remaining(t)); return min==Long.MAX_VALUE?0:min; }
    public boolean strict() { for(TimedBlock t:active()) if(t.strict) return true; return false; }
    public Set<String> apps(boolean strict) { Set<String> result=new HashSet<>(); for(TimedBlock t:active()) if(t.app && !t.shorts && t.strict==strict) result.add(t.target); return result; }
    public Set<String> apps() { Set<String> result=apps(false); result.addAll(apps(true)); return result; }
    public Set<String> domains() { Set<String> result=new HashSet<>(); for(TimedBlock t:active()) if(!t.app && !t.shorts) result.add(t.target); return result; }
    public long normalAppRemaining(String pkg) { long left=0; for(TimedBlock t:active()) if(t.app && !t.shorts && !t.strict && t.target.equals(pkg)) left=Math.max(left,remaining(t)); return left; }
    public boolean activeTarget(boolean app,String target) { for(TimedBlock t:active()) if(t.app==app && t.target.equals(target)) return true; return false; }
    public long shortsRemaining() { return shortsRemaining(true); }
    public long webShortsRemaining() { return shortsRemaining(false); }
    private long shortsRemaining(boolean app) { long left=0; for(TimedBlock t:active()) if(t.shorts && t.app==app) left=Math.max(left,remaining(t)); return left; }
    public void start(Set<String> domains,Set<String> apps,int minutes,boolean strict) { start(domains,apps,minutes,strict,false); }
    public void start(Set<String> domains,Set<String> apps,int minutes,boolean strict,boolean shorts) {
        start(domains,apps,minutes,strict,shorts,false);
    }
    public void start(Set<String> domains,Set<String> apps,int minutes,boolean strict,boolean shorts,boolean webShorts) {
        if(shorts && (!apps.contains(ScreenRules.YOUTUBE) || strict)) throw new IllegalArgumentException("Shorts-only blocking requires YouTube and Normal mode.");
        if((shorts || webShorts) && !FocusScreenService.active()) throw new IllegalStateException("Enable Screen-aware blocking before starting a Shorts timer.");
        List<TimedBlock> previous;
        Set<String> created=new HashSet<>();
        synchronized(FocusStore.class) {
            if(minutes<1 || minutes>525600 || domains.isEmpty() && apps.isEmpty() && !webShorts) throw new IllegalArgumentException("Choose an app or website and 1 to 525600 minutes.");
            SessionMode.validate(strict,ManagedProtection.ready(context),strict || NormalAppBlocker.ready(context),!apps.isEmpty());
            for(String pkg:apps) if(CallSafety.protectedApp(context,pkg)) throw new IllegalArgumentException("Phone and calling apps are excluded to protect calls.");
            for(String domain:domains) if(CallDomains.carrier(domain)) throw new IllegalArgumentException("Carrier calling domains cannot be blocked.");
            previous=active(); List<TimedBlock> updated=new ArrayList<>(previous);
            if(webShorts && activeTarget(false,ScreenRules.WEB_SHORTS)) throw new IllegalStateException("YouTube Shorts (web) already has a timer.");
            for(TimedBlock t:previous) if(t.app?apps.contains(t.target):domains.contains(t.target)) throw new IllegalStateException("This item already has its own timer. Choose another item.");
            long duration=minutes*60000L,wall=System.currentTimeMillis()+duration,elapsed=SystemClock.elapsedRealtime()+duration; int currentBoot=boot();
            for(String pkg:apps) updated.add(new TimedBlock(UUID.randomUUID().toString(),pkg,true,strict,wall,elapsed,currentBoot,shorts && ScreenRules.YOUTUBE.equals(pkg)));
            for(String host:domains) updated.add(new TimedBlock(UUID.randomUUID().toString(),host,false,strict,wall,elapsed,currentBoot));
            if(webShorts) updated.add(new TimedBlock(UUID.randomUUID().toString(),ScreenRules.WEB_SHORTS,false,strict,wall,elapsed,currentBoot,true));
            for(TimedBlock t:updated) if(!previous.contains(t)) created.add(t.id);
            write(updated);
        }
            try {
                ManagedProtection.reconcile(context);
                if(strict && !ManagedProtection.enforced(context)) throw new IllegalStateException("Android did not confirm strict protection.");
                SessionReceiver.schedule(context);
            } catch(RuntimeException e) {
                synchronized(FocusStore.class) { List<TimedBlock> rollback=read(); rollback.removeIf(t->created.contains(t.id)); write(rollback); }
                try { ManagedProtection.reconcile(context); SessionReceiver.schedule(context); } catch(RuntimeException cleanup) { e.addSuppressed(cleanup); }
                throw e;
            }
    }
    public boolean blocks(String host) { for(String d:domains()) if(DomainRules.covers(d,host)) return true; return false; }
    public boolean enabled() { return prefs.getBoolean("enabled",false); }
    public void enable() { prefs.edit().putBoolean("enabled",true).apply(); }
}
