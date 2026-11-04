package com.stillfocus.app;

/** Setup is a one-time journey; revocation later is handled by the affected feature. */
public final class SetupState {
    private boolean completed;
    public SetupState(boolean completed) { this.completed=completed; }
    public boolean needsSetup() { return !completed; }
    public boolean observe(boolean vpn,boolean usage,boolean overlay) {
        if(!completed && vpn && usage && overlay) { completed=true; return true; }
        return false;
    }
}
