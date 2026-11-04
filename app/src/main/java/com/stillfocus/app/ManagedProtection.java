package com.stillfocus.app;

import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.os.Build;
import android.os.UserManager;
import java.util.*;

/** Journal policies and reconcile the union of timers and strict schedules. */
public final class ManagedProtection {
    private static String applied;
    public static boolean ready(Context c) {
        return Build.VERSION.SDK_INT>=30 && c.getSystemService(DevicePolicyManager.class).isDeviceOwnerApp(c.getPackageName());
    }
    public static synchronized void reconcile(Context c) {
        if(Build.VERSION.SDK_INT<30 || !ready(c)) return;
        DevicePolicyManager dpm=c.getSystemService(DevicePolicyManager.class);
        ComponentName admin=new ComponentName(c,FocusAdminReceiver.class);
        android.content.SharedPreferences state=c.getSharedPreferences("management",0);
        FocusStore session=new FocusStore(c);
        Set<String> desired=new ScheduleStore(c).activeApps(true,ScheduleStore.now());
        boolean timerActive=session.remaining()>0 && session.strict();
        boolean active=timerActive || !desired.isEmpty();
        if(timerActive) desired.addAll(session.apps(true));
        desired.removeIf(pkg->CallSafety.protectedApp(c,pkg));
        String key=active+":"+new TreeSet<>(desired);
        boolean locked=state.getBoolean("locked",false);
        if(key.equals(applied) && active==locked) {
            boolean confirmed=!active || enforced(c);
            for(String pkg:desired) {
                try { confirmed &= dpm.isPackageSuspended(admin,pkg); }
                catch(android.content.pm.PackageManager.NameNotFoundException removed) { confirmed=false; }
            }
            if(confirmed) return;
        }
        applied=null; // A partial failure must invalidate the cache before rollback.
        Set<String> owned=new HashSet<>(state.getStringSet("ownedApps",state.getStringSet("sessionApps",Collections.emptySet())));
        if(active && !locked) {
            if(!state.edit().putStringSet("baseline",new HashSet<>(dpm.getUserControlDisabledPackages(admin)))
                .putBoolean("dateRestricted",dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_CONFIG_DATE_TIME))
                .putBoolean("locked",true).commit()) throw new IllegalStateException("Could not save management state");
        }
        Set<String> restore=new HashSet<>(owned); restore.removeAll(desired);
        Set<String> actuallySuspended=new HashSet<>();
        for(String pkg:restore) {
            try { if(dpm.isPackageSuspended(admin,pkg)) actuallySuspended.add(pkg); }
            catch(android.content.pm.PackageManager.NameNotFoundException removed) { }
        }
        if(!actuallySuspended.isEmpty() && dpm.setPackagesSuspended(admin,actuallySuspended.toArray(new String[0]),false).length>0)
            throw new IllegalStateException("Could not restore paused apps yet");
        owned.removeAll(restore);
        if(active) {
            dpm.setUninstallBlocked(admin,c.getPackageName(),true);
            Set<String> controls=new HashSet<>(state.getStringSet("baseline",Collections.emptySet())); controls.add(c.getPackageName());
            dpm.setUserControlDisabledPackages(admin,new ArrayList<>(controls));
            dpm.addUserRestriction(admin,UserManager.DISALLOW_CONFIG_DATE_TIME);
            for(String pkg:desired) if(!owned.contains(pkg)) {
                try { if(!dpm.isPackageSuspended(admin,pkg)) owned.add(pkg); }
                catch(android.content.pm.PackageManager.NameNotFoundException removed) { }
            }
            // Write intended changes first, so a crash cannot strand a suspended app.
            if(!state.edit().putStringSet("ownedApps",owned).remove("sessionApps").commit()) throw new IllegalStateException("Could not save paused apps");
            if(!owned.isEmpty()) {
                Set<String> installed=new HashSet<>();
                for(String pkg:owned) {
                    try { c.getPackageManager().getApplicationInfo(pkg,0); installed.add(pkg); }
                    catch(android.content.pm.PackageManager.NameNotFoundException removed) { }
                }
                String[] failed=dpm.setPackagesSuspended(admin,installed.toArray(new String[0]),true);
                if(failed.length>0) throw new IllegalStateException("Android cannot pause these apps: "+String.join(", ",failed));
            }
            for(String pkg:desired) {
                try {
                    if(!dpm.isPackageSuspended(admin,pkg)) throw new IllegalStateException("Android did not confirm app suspension: "+pkg);
                } catch(android.content.pm.PackageManager.NameNotFoundException removed) {
                    throw new IllegalStateException("Selected app is no longer installed: "+pkg);
                }
            }
        } else if(locked) {
            dpm.setUninstallBlocked(admin,c.getPackageName(),false);
            dpm.setUserControlDisabledPackages(admin,new ArrayList<>(state.getStringSet("baseline",Collections.emptySet())));
            if(!state.getBoolean("dateRestricted",false)) dpm.clearUserRestriction(admin,UserManager.DISALLOW_CONFIG_DATE_TIME);
            if(!state.edit().putBoolean("locked",false).remove("ownedApps").remove("sessionApps").commit()) throw new IllegalStateException("Could not save restored state");
        }
        applied=key;
    }
    public static boolean locked(Context c) {
        return ready(c) && c.getSystemService(DevicePolicyManager.class).isUninstallBlocked(new ComponentName(c,FocusAdminReceiver.class),c.getPackageName());
    }
    public static boolean enforced(Context c) {
        if(Build.VERSION.SDK_INT<30 || !ready(c)) return false;
        DevicePolicyManager dpm=c.getSystemService(DevicePolicyManager.class);
        ComponentName admin=new ComponentName(c,FocusAdminReceiver.class);
        return dpm.isUninstallBlocked(admin,c.getPackageName()) && dpm.getUserControlDisabledPackages(admin).contains(c.getPackageName());
    }
    @SuppressWarnings("deprecation")
    public static void releaseOwnership(Context c) {
        FocusStore session=new FocusStore(c); ScheduleStore schedules=new ScheduleStore(c);
        if(session.remaining()>0 && session.strict() || schedules.activeStrict()) throw new IllegalStateException("Wait for strict blocks to finish.");
        if(schedules.enabledStrict()) throw new IllegalStateException("Disable strict schedules before removing device-owner setup.");
        reconcile(c);
        if(ready(c)) c.getSystemService(DevicePolicyManager.class).clearDeviceOwnerApp(c.getPackageName());
    }
}
