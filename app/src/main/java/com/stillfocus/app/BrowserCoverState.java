package com.stillfocus.app;

/** Retains only a window identity/deadline while its address is being edited. */
public final class BrowserCoverState {
    private String pkg="";
    private int window=-1;
    private long until;
    public void cover(String pkg,int window,long remaining,long now) {
        this.pkg=pkg; this.window=window;
        until=remaining==Long.MAX_VALUE?Long.MAX_VALUE:now+Math.max(0,remaining);
    }
    public boolean keep(String pkg,int window,long now) {
        return this.pkg.equals(pkg) && this.window==window && now<until;
    }
    public void clear() { pkg=""; window=-1; until=0; }
}
