package com.stillfocus.app;

import android.app.*;
import android.app.usage.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.Locale;

/** User-consented window events or usage polling with a shared focus overlay. */
public final class NormalAppBlocker {
    private final Context context;
    private final boolean eventDriven;
    private final FocusStore store;
    private final ScheduleStore schedules;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ForegroundApp foreground=new ForegroundApp();
    private final BlockedAppExit exit=new BlockedAppExit();
    private final WindowManager windows;
    private LinearLayout cover,fadingCover;
    private boolean returningHome;
    private long returnRequestedAt;
    private TextView countdown;
    private String covered="";
    private long since,shownResume=-1;
    private boolean stopped;
    public static boolean usageAllowed(Context c) {
        AppOpsManager ops=c.getSystemService(AppOpsManager.class);
        int mode=ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),c.getPackageName());
        return mode==AppOpsManager.MODE_ALLOWED;
    }
    public static boolean ready(Context c) { return (FocusAccessibilityService.active() || usageAllowed(c)) && Settings.canDrawOverlays(c); }
    public NormalAppBlocker(Context c) { this(c,false); }
    public NormalAppBlocker(Context c,boolean eventDriven) { this.eventDriven=eventDriven; context=c; store=new FocusStore(c); schedules=new ScheduleStore(c); windows=c.getSystemService(WindowManager.class); }
    private final Runnable poll=new Runnable() {
        @Override public void run() {
            if(stopped) return;
            try { update(); } catch(RuntimeException e) { hide(); }
            handler.postDelayed(this,200);
        }
    };
    public void start() { handler.post(poll); }
    public void stop() { stopped=true; handler.removeCallbacks(poll); hide(); }
    public void windowChanged(String pkg) {
        if(stopped || !eventDriven) return;
        if(pkg.isEmpty()) { foreground.clear(); hide(); exit.clear(); return; }
        foreground.event(SystemClock.uptimeMillis(),pkg,true);
        try { update(); } catch(RuntimeException e) { hide(); }
    }
    private void update() {
        // Only one controller owns overlays. Polling resumes when Accessibility disconnects.
        if(!eventDriven && FocusAccessibilityService.active()) { hide(); foreground.clear(); exit.clear(); since=0; return; }

        java.time.ZonedDateTime local=ScheduleStore.now();
        java.util.Set<String> paused=schedules.activeApps(false,local);
        paused.addAll(store.apps(false));
        paused.removeIf(pkg->CallSafety.protectedApp(context,pkg));
        if(paused.isEmpty() || !ready(context) || CallSafety.callAudio(context)) {
            hide(); foreground.clear(); exit.clear(); since=0; return;
        }
        if(!eventDriven) {
        long now=System.currentTimeMillis();
        if(since==0 || since>now) { foreground.clear(); since=now-15*60*1000L; }
        UsageEvents events=context.getSystemService(UsageStatsManager.class).queryEvents(since,now);
        if(events!=null) {
            UsageEvents.Event event=new UsageEvents.Event();
            while(events.hasNextEvent()) {
                events.getNextEvent(event);
                int kind=event.getEventType();
                if(kind==UsageEvents.Event.MOVE_TO_FOREGROUND) foreground.event(event.getTimeStamp(),event.getPackageName(),event.getClassName(),true);
                else if(kind==UsageEvents.Event.MOVE_TO_BACKGROUND) foreground.event(event.getTimeStamp(),event.getPackageName(),event.getClassName(),false);
            }
        }
        since=Math.max(0,now-30000);
        }
        boolean unlocked=!context.getSystemService(KeyguardManager.class).isKeyguardLocked()
            && context.getSystemService(PowerManager.class).isInteractive();
        String pkg=foreground.current();
        if(unlocked && pkg.isEmpty() && returningHome && SystemClock.elapsedRealtime()-returnRequestedAt<1000) return;
        if(!unlocked || pkg.equals(context.getPackageName()) || !paused.contains(pkg)) {
            boolean home=false;
            if(unlocked && (returningHome || fadingCover!=null)) {
                android.content.ComponentName launcher=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).resolveActivity(context.getPackageManager());
                home=launcher!=null && pkg.equals(launcher.getPackageName());
            }
            hide(home); exit.clear(); return;
        }
        if(cover==null || !covered.equals(pkg) || shownResume!=foreground.resumedAt() || !cover.isAttachedToWindow()) { show(pkg); shownResume=foreground.resumedAt(); }
        // An overlay alone leaves the selected activity resumed under Recents.
        // Request Home as well; never close tasks, suppress system UI, or interfere with calls.
        if(exit.shouldExit(pkg,foreground.resumedAt(),SystemClock.elapsedRealtime())) goHome();
        if(countdown!=null) {
            long remaining=schedules.normalRemaining(pkg,local);
            remaining=Math.max(remaining,store.normalAppRemaining(pkg));
            long seconds=(remaining+999)/1000;
            countdown.setText(String.format(Locale.getDefault(),"Available in %02d:%02d",seconds/60,seconds%60));
        }
    }
    private int dp(int value) { return (int)(value*context.getResources().getDisplayMetrics().density+.5f); }
    private void goHome() {
        if(eventDriven && FocusAccessibilityService.home()) {
            returningHome=true; returnRequestedAt=SystemClock.elapsedRealtime(); return;
        }

        Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { context.startActivity(intent,ActivityOptions.makeCustomAnimation(context,R.anim.focus_home_enter,R.anim.focus_app_exit).toBundle()); returningHome=true; returnRequestedAt=SystemClock.elapsedRealtime(); } catch(RuntimeException ignored) { /* Overlay remains as fallback. */ }
    }
    private TextView text(String value,int size) {
        TextView t=new TextView(context); t.setText(value); t.setTextSize(size); t.setTextColor(FocusUi.INK);
        t.setGravity(Gravity.CENTER); t.setPadding(dp(8),dp(14),dp(8),dp(14)); return t;
    }
    private void show(String pkg) {
        hide();
        String label=pkg;
        try { label=context.getPackageManager().getApplicationLabel(context.getPackageManager().getApplicationInfo(pkg,0)).toString(); }
        catch(android.content.pm.PackageManager.NameNotFoundException ignored) { }
        cover=new LinearLayout(context); cover.setOrientation(LinearLayout.VERTICAL); cover.setGravity(Gravity.CENTER);
        cover.setPadding(dp(28),dp(24),dp(28),dp(24)); cover.setBackgroundColor(FocusUi.PAPER);
        cover.addView(text("Still Focus · focus session",17)); cover.addView(text(label+" is on a break",30));
        long seconds=(Math.max(store.normalAppRemaining(pkg),schedules.normalRemaining(pkg,ScheduleStore.now()))+999)/1000;
        String availability=String.format(Locale.getDefault(),"Available in %02d:%02d",seconds/60,seconds%60);
        countdown=text(availability,24); cover.addView(countdown);
        Toast.makeText(context,label+" is on a break. "+availability,Toast.LENGTH_SHORT).show();
        cover.addView(text("Timer active. Returning Home.",16));
        Button home=new Button(context); home.setText("Back to Home"); home.setAllCaps(false); FocusUi.button(home,true);
        home.setOnClickListener(v -> goHome()); cover.addView(home);
        cover.addView(text("Normal mode · Android Settings can stop Still Focus. Background audio may continue.",13));
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        params.setTitle("Still Focus session reminder");
        // No fullscreen/lockscreen flags: system navigation and Settings remain accessible.
        windows.addView(cover,params); covered=pkg;
    }

    private void removeCover(LinearLayout view) {
        if(view==null) return;
        view.animate().cancel();
        try { windows.removeView(view); } catch(RuntimeException ignored) { }
    }
    private void hide() { hide(false); }
    private void hide(boolean smooth) {
        // A call, lock, timer expiry or service stop removes all overlays immediately.
        if(!smooth) { removeCover(fadingCover); fadingCover=null; }
        LinearLayout previous=cover;
        cover=null; countdown=null; covered=""; returningHome=false;
        if(previous==null) return;
        if(smooth && android.animation.ValueAnimator.areAnimatorsEnabled()) {
            removeCover(fadingCover); fadingCover=previous;
            // All corners converge just above the bottom centre, revealing Home beneath.
            previous.setPivotX(previous.getWidth() * .5f);
            previous.setPivotY(previous.getHeight() * .86f);
            previous.setBackground(FocusUi.surface(context,FocusUi.PAPER,28));
            previous.animate().scaleX(0f).scaleY(0f).setDuration(420)
                .setInterpolator(new android.view.animation.PathInterpolator(.4f,0f,.2f,1f)).withEndAction(()-> {
                removeCover(previous); if(fadingCover==previous) fadingCover=null;
            }).start();
        } else removeCover(previous);
    }
}
