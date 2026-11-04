package com.stillfocus.app;

/** Monotonic within a boot; persisted calendar deadline across boots. */
public final class SessionDeadline {
    private SessionDeadline() { }
    public static long remaining(long wallEnd,long elapsedEnd,int savedBoot,int currentBoot,long wallNow,long elapsedNow) {
        long left=savedBoot>=0 && savedBoot==currentBoot?elapsedEnd-elapsedNow:wallEnd-wallNow;
        return Math.max(0,left);
    }
}
