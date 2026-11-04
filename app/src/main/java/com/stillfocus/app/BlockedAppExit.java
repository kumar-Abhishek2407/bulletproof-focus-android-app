package com.stillfocus.app;

/** Immediate Home request on detection, followed by bounded retries. */
public final class BlockedAppExit {
    private String lastPackage="";
    private long lastResume=-1,firstSeen=-1,lastAttempt=-1;
    public boolean shouldExit(String pkg,long resumedAt,long elapsedNow) {
        if(pkg==null || pkg.isEmpty()) { clear(); return false; }
        if(!pkg.equals(lastPackage) || elapsedNow<firstSeen) {
            lastPackage=pkg; lastResume=resumedAt; firstSeen=elapsedNow; lastAttempt=-1;
        }
        if(lastAttempt>=0 && elapsedNow-lastAttempt<1500) return false;
        lastAttempt=elapsedNow; return true;
    }
    public void clear() { lastPackage=""; lastResume=-1; firstSeen=-1; lastAttempt=-1; }
}
