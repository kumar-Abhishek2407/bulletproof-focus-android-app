package com.stillfocus.app;

/** In-memory foreground tracking; no app-use history is stored. */
public final class ForegroundApp {
    private String current="";
    private long timestamp=-1;
    private String activity="";
    private long resumedAt=-1;
    public void event(long time,String pkg,boolean resumed) {
        event(time,pkg,pkg,resumed);
    }
    public void event(long time,String pkg,String activity,boolean resumed) {
        if(time<timestamp || pkg==null) return;
        timestamp=time;
        String key=activity==null?pkg:activity;
        if(resumed) { current=pkg; this.activity=key; resumedAt=time; }
        else if(current.equals(pkg) && this.activity.equals(key)) current="";
    }
    public String current() { return current; }
    public long resumedAt() { return resumedAt; }
    // Window events may not repeat when a schedule activates while the user stays in one app.
    public void idle(boolean eventDriven) { if(!eventDriven) clear(); }
    public void clear() { current=""; activity=""; timestamp=-1; resumedAt=-1; }
}
