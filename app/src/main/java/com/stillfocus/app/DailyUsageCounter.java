package com.stillfocus.app;
import java.util.*;
/** Foreground intervals clipped to one local day; no background-media accounting. */
public final class DailyUsageCounter {
    private final long start,end;
    private long cursor;
    private String pkg="",activity="";
    private boolean interactive=true,locked;
    private final Map<String,Long> totals=new HashMap<>();
    public DailyUsageCounter(long start,long end) { this.start=start; this.end=end; cursor=start; }
    private void advance(long time) {
        long next=Math.max(cursor,Math.min(end,time));
        if(interactive && !locked && !pkg.isEmpty() && next>cursor) totals.merge(pkg,next-cursor,Long::sum);
        cursor=next;
    }
    public void resume(long at,String pkg,String activity) { advance(at); this.pkg=pkg==null?"":pkg; this.activity=activity==null?"":activity; }
    public void pause(long at,String pkg,String activity) { advance(at); if(this.pkg.equals(pkg) && (activity==null || this.activity.equals(activity))) { this.pkg=""; this.activity=""; } }
    public void screen(long at,boolean on) { advance(at); interactive=on; }
    public void lock(long at,boolean locked) { advance(at); this.locked=locked; }
    public void stop(long at) { advance(at); pkg=""; activity=""; }
    public Map<String,Long> finish() { advance(end); return new HashMap<>(totals); }
    public static boolean exhausted(long used,int minutes,boolean latched) { return latched || used>=minutes*60000L; }
}
