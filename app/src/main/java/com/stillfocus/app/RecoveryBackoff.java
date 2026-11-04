package com.stillfocus.app;
/** Bounded reconnect delays; stable connections reset the retry budget. */
public final class RecoveryBackoff {
    private long delay=1000;
    public long next(long connectedMillis) {
        if(connectedMillis>=30000) delay=1000;
        long result=delay; delay=Math.min(30000,delay*2); return result;
    }
}
