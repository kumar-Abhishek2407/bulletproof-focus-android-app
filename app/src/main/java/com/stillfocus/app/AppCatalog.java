package com.stillfocus.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.LruCache;
import android.widget.ImageView;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** App labels and icons are presentation data; loading them must never stall navigation. */
final class AppCatalog {
    static final class Entry {
        final String packageName,label;
        Entry(String packageName,String label) { this.packageName=packageName; this.label=label; }
    }
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private static final ExecutorService ICON_WORKER=Executors.newSingleThreadExecutor();
    private static final LruCache<String,Drawable.ConstantState> ICONS=new LruCache<>(96);
    private static final LruCache<String,String> LABELS=new LruCache<>(256);
    private static List<Entry> cached=Collections.emptyList();
    private static long loadedAt;
    private AppCatalog() { }

    static void load(Context context,Set<String> include,Consumer<List<Entry>> callback) {
        Context app=context.getApplicationContext(); Set<String> extras=new HashSet<>(include);
        WORKER.execute(()-> {
            PackageManager pm=app.getPackageManager();
            try { if(loadedAt==0 || SystemClock.elapsedRealtime()-loadedAt>60000) {
                Map<String,Entry> entries=new HashMap<>();
                ComponentName home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).resolveActivity(pm);
                for(ResolveInfo info:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)) {
                    if(info.activityInfo==null) continue;
                    String pkg=info.activityInfo.packageName;
                    if(pkg.equals(app.getPackageName()) || pkg.equals("com.android.settings") || home!=null && pkg.equals(home.getPackageName()) || CallSafety.protectedApp(app,pkg)) continue;
                    String label;
                    try { label=info.loadLabel(pm).toString(); } catch(RuntimeException unavailable) { label=loadLabel(pm,pkg); }
                    LABELS.put(pkg,label); entries.put(pkg,new Entry(pkg,label));
                }
                cached=new ArrayList<>(entries.values()); loadedAt=SystemClock.elapsedRealtime();
            } } catch(RuntimeException unavailable) { /* Keep the last snapshot; still complete the request. */ }
            Map<String,Entry> entries=new HashMap<>(); for(Entry entry:cached) entries.put(entry.packageName,entry);
            for(String pkg:extras) if(!entries.containsKey(pkg)) entries.put(pkg,new Entry(pkg,loadLabel(pm,pkg)));
            List<Entry> result=new ArrayList<>(entries.values());
            result.sort(Comparator.comparing(e->e.label,String.CASE_INSENSITIVE_ORDER));
            MAIN.post(()->callback.accept(result));
        });
    }
    private static String loadLabel(PackageManager pm,String pkg) {
        String label=LABELS.get(pkg); if(label!=null) return label;
        try { label=pm.getApplicationLabel(pm.getApplicationInfo(pkg,0)).toString(); }
        catch(PackageManager.NameNotFoundException | RuntimeException e) { label="Unavailable app"; }
        LABELS.put(pkg,label); return label;
    }
    static void icon(ImageView view,String pkg) {
        view.setTag(pkg); Drawable.ConstantState saved=ICONS.get(pkg);
        if(saved!=null) {
            try { view.setImageDrawable(saved.newDrawable(view.getResources())); return; }
            catch(RuntimeException unavailable) { ICONS.remove(pkg); }
        }
        Context app=view.getContext().getApplicationContext(); WeakReference<ImageView> target=new WeakReference<>(view);
        ICON_WORKER.execute(()-> {
            if(target.get()==null) return;
            Drawable.ConstantState current=ICONS.get(pkg);
            if(current!=null) { MAIN.post(()->bindIcon(target,pkg,current,null)); return; }
            Drawable icon;
            try { icon=app.getPackageManager().getApplicationIcon(pkg); }
            catch(PackageManager.NameNotFoundException | RuntimeException e) {
                try { icon=app.getPackageManager().getDefaultActivityIcon(); } catch(RuntimeException unavailable) { return; }
            }
            Drawable.ConstantState state=icon.getConstantState(); if(state!=null) ICONS.put(pkg,state);
            Drawable loaded=icon;
            MAIN.post(()->bindIcon(target,pkg,state,loaded));
        });
    }
    private static void bindIcon(WeakReference<ImageView> target,String pkg,Drawable.ConstantState state,Drawable fallback) {
        ImageView current=target.get(); if(current==null || !pkg.equals(current.getTag())) return;
        try { current.setImageDrawable(state==null?fallback:state.newDrawable(current.getResources())); }
        catch(RuntimeException unavailable) { current.setImageDrawable(null); }
    }
}
