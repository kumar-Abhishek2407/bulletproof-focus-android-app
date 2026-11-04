package com.stillfocus.app;

import java.time.*;
import java.time.zone.ZoneOffsetTransition;

/** Daily local-time window; start inclusive, end exclusive, including across midnight. */
public final class DailyWindow {
    public final int start,end;
    public DailyWindow(int start,int end) {
        if(start<0 || start>=1440 || end<0 || end>=1440 || start==end) throw new IllegalArgumentException("Choose different start and end times.");
        this.start=start; this.end=end;
    }
    public boolean active(ZonedDateTime now) { return activeMinute(now.getHour()*60+now.getMinute()); }
    public boolean activeMinute(int minute) { return start<end?minute>=start && minute<end:minute>=start || minute<end; }
    public long nextBoundary(ZonedDateTime now) {
        long current=now.toInstant().toEpochMilli(),best=Long.MAX_VALUE;
        for(int day=0;day<=2;day++) for(int minute:new int[]{start,end}) {
            ZonedDateTime candidate=now.toLocalDate().plusDays(day).atTime(minute/60,minute%60).atZone(now.getZone());
            for(ZonedDateTime option:new ZonedDateTime[]{candidate,candidate.withLaterOffsetAtOverlap()}) {
                long value=option.toInstant().toEpochMilli(); if(value>current) best=Math.min(best,value);
            }
        }
        ZoneOffsetTransition transition=now.getZone().getRules().nextTransition(now.toInstant());
        if(transition!=null) { long value=transition.getInstant().toEpochMilli(); if(value>current) best=Math.min(best,value); }
        return best;
    }
    public long remaining(ZonedDateTime now) {
        if(!active(now)) return 0;
        ZonedDateTime cursor=now;
        for(int i=0;i<10;i++) {
            long boundary=nextBoundary(cursor);
            cursor=Instant.ofEpochMilli(boundary).atZone(now.getZone());
            if(!active(cursor)) return boundary-now.toInstant().toEpochMilli();
        }
        throw new IllegalStateException("Could not determine schedule end");
    }
    public static boolean coversWholeDay(Iterable<DailyWindow> windows) {
        boolean[] covered=new boolean[1440];
        for(DailyWindow w:windows) for(int i=0;i<1440;i++) covered[i]|=w.activeMinute(i);
        for(boolean minute:covered) if(!minute) return false;
        return true;
    }
}
