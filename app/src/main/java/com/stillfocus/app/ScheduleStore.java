package com.stillfocus.app;

import android.content.*;
import org.json.*;
import java.time.ZonedDateTime;
import java.util.*;

public final class ScheduleStore {
    private final Context context;
    private final android.content.SharedPreferences prefs;
    private static String cachedRaw;
    private static List<AppSchedule> cached;
    public ScheduleStore(Context c) { context=c.getApplicationContext(); prefs=context.getSharedPreferences("schedules",0); }
    public static ZonedDateTime now() { return ZonedDateTime.now(); }
    public List<AppSchedule> all() {
        String raw=prefs.getString("entries","[]");
        synchronized(ScheduleStore.class) {
            if(!raw.equals(cachedRaw)) {
                List<AppSchedule> result=new ArrayList<>();
                try {
                    JSONArray rows=new JSONArray(raw);
                    for(int i=0;i<rows.length();i++) {
                        JSONObject j=rows.getJSONObject(i); Set<String> apps=new HashSet<>(); JSONArray a=j.getJSONArray("apps");
                        for(int k=0;k<a.length();k++) apps.add(a.getString(k));
                        result.add(new AppSchedule(j.getString("id"),j.getString("name"),apps,j.getInt("start"),j.getInt("end"),j.getBoolean("strict"),j.getBoolean("enabled")));
                    }
                } catch(JSONException e) { throw new IllegalStateException("Could not read saved schedules",e); }
                cached=result; cachedRaw=raw;
            }
            return new ArrayList<>(cached);
        }
    }
    private void write(List<AppSchedule> entries) {
        JSONArray rows=new JSONArray();
        try {
            for(AppSchedule s:entries) {
                JSONObject j=new JSONObject(); j.put("id",s.id); j.put("name",s.name); j.put("apps",new JSONArray(new TreeSet<>(s.apps)));
                j.put("start",s.window.start); j.put("end",s.window.end); j.put("strict",s.strict); j.put("enabled",s.enabled); rows.put(j);
            }
        } catch(JSONException e) { throw new IllegalStateException(e); }
        if(!prefs.edit().putString("entries",rows.toString()).commit()) throw new IllegalStateException("Could not save schedule");
    }
    public AppSchedule find(String id) { for(AppSchedule s:all()) if(s.id.equals(id)) return s; return null; }
    public Set<String> activeApps(boolean strict,ZonedDateTime now) {
        return AppSchedule.activeApps(all(),strict,now);
    }
    public boolean activeStrict() { return !activeApps(true,now()).isEmpty(); }
    public boolean enabledStrict() { for(AppSchedule s:all()) if(s.enabled && s.strict) return true; return false; }
    public void save(AppSchedule next) { change(next.id,next); }
    public void delete(String id) { change(id,null); }
    private synchronized void change(String id,AppSchedule next) {
        List<AppSchedule> previous=all(),updated=new ArrayList<>();
        for(AppSchedule s:previous) {
            if(s.id.equals(id)) {
                if(s.strict && s.active(now())) throw new IllegalStateException("This strict schedule is active. Wait until its end time to change it.");
            } else updated.add(s);
        }
        if(next!=null) {
            if(next.enabled) for(String pkg:next.apps) if(CallSafety.protectedApp(context,pkg)) throw new IllegalArgumentException("Phone and calling apps are excluded to protect calls.");
            if(next.enabled) SessionMode.validate(next.strict,ManagedProtection.ready(context),next.strict || NormalAppBlocker.ready(context),true);
            updated.add(next);
        }
        if(updated.size()>30) throw new IllegalArgumentException("You can save up to 30 schedules.");
        List<DailyWindow> strictWindows=new ArrayList<>();
        for(AppSchedule s:updated) if(s.enabled && s.strict) strictWindows.add(s.window);
        if(DailyWindow.coversWholeDay(strictWindows)) throw new IllegalArgumentException("Leave a daily gap between strict schedules so device management can be removed.");
        write(updated);
        try {
            ManagedProtection.reconcile(context);
            if(next!=null && next.strict && next.active(now()) && !ManagedProtection.enforced(context)) throw new IllegalStateException("Android did not confirm strict protection.");
            ScheduleReceiver.schedule(context);
        }
        catch(RuntimeException e) {
            write(previous);
            try { ManagedProtection.reconcile(context); ScheduleReceiver.schedule(context); } catch(RuntimeException cleanup) { e.addSuppressed(cleanup); }
            throw e;
        }
    }
    public long normalRemaining(String pkg,ZonedDateTime now) {
        long remaining=0;
        for(AppSchedule s:all()) if(!s.strict && s.apps.contains(pkg) && s.active(now)) remaining=Math.max(remaining,s.window.remaining(now));
        return remaining;
    }
}
