package com.stillfocus.app;

import java.time.ZonedDateTime;
import java.util.*;

public final class AppSchedule {
    public final String id,name;
    public final Set<String> apps;
    public final DailyWindow window;
    public final boolean strict,enabled;
    public AppSchedule(String id,String name,Set<String> apps,int start,int end,boolean strict,boolean enabled) {
        if(apps.isEmpty()) throw new IllegalArgumentException("Choose at least one app.");
        this.id=id; this.name=name.trim().isEmpty()?"Daily pause":name.trim();
        this.apps=Collections.unmodifiableSet(new HashSet<>(apps)); window=new DailyWindow(start,end);
        this.strict=strict; this.enabled=enabled;
    }
    public boolean active(ZonedDateTime now) { return enabled && window.active(now); }
    public static Set<String> activeApps(Iterable<AppSchedule> schedules,boolean strict,ZonedDateTime now) {
        Set<String> result=new HashSet<>();
        for(AppSchedule s:schedules) if(s.strict==strict && s.active(now)) result.addAll(s.apps);
        return result;
    }
}
