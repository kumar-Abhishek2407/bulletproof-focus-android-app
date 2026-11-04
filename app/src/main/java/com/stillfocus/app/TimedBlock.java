package com.stillfocus.app;

public final class TimedBlock {
    public final String id,target;
    public final boolean app,strict;
    public final long wallEnd,elapsedEnd;
    public final int boot;
    public TimedBlock(String id,String target,boolean app,boolean strict,long wallEnd,long elapsedEnd,int boot) {
        this.id=id; this.target=target; this.app=app; this.strict=strict; this.wallEnd=wallEnd; this.elapsedEnd=elapsedEnd; this.boot=boot;
    }
    public long remaining(int currentBoot,long wallNow,long elapsedNow) {
        return SessionDeadline.remaining(wallEnd,elapsedEnd,boot,currentBoot,wallNow,elapsedNow);
    }
}
