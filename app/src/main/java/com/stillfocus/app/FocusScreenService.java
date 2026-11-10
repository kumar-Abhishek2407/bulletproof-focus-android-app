package com.stillfocus.app;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.*;
import android.view.accessibility.*;
import android.widget.Toast;
import java.util.*;

/** Separately consented, package-restricted browser address / Shorts screen detection. */
public final class FocusScreenService extends AccessibilityService {
    private static FocusScreenService connected;
    public static volatile String status="Screen-aware blocking is off";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private String lastPackage="";
    private int lastWindowId=-1;
    private long lastRedirect,lastMediaAttempt;
    private boolean shortsBackPending;
    private boolean stopped;
    private BrowserPageCover browserCover;
    private final BrowserCoverState coveredPage=new BrowserCoverState();
    public static boolean active() { return connected!=null && !PaymentMode.enabled(connected) && consented(connected); }
    private static boolean consented(Context c) { return c.getSharedPreferences("screenConsent",0).getBoolean("accepted",false); }
    public static void consent(Context c) { c.getSharedPreferences("screenConsent",0).edit().putBoolean("accepted",true).apply(); }
    public static void turnOff(Context c) {
        c.getSharedPreferences("screenConsent",0).edit().putBoolean("accepted",false).apply();
        if(connected!=null) { connected.handler.removeCallbacksAndMessages(null); connected.hideBrowser(); connected.disableSelf(); connected=null; }
        status="Screen-aware blocking is off";
    }
    public static void pauseForPayment() {
        if(connected!=null) { connected.stopped=true; connected.handler.removeCallbacksAndMessages(null); connected.hideBrowser(); connected.disableSelf(); connected=null; }
        status="Payment mode: screen-aware blocking is off";
    }
    @Override protected void onServiceConnected() {
        if(PaymentMode.enabled(this)) { disableSelf(); return; }
        if(!consented(this)) { disableSelf(); return; }
        connected=this; stopped=false; lastPackage=""; lastWindowId=-1; browserCover=new BrowserPageCover(this); status="Screen-aware blocking connected";
        new Thread(()-> { try { WebsiteFilter.load(this); } catch(java.io.IOException e) { status="Browser rules unavailable; reopen Still Focus"; } },"screen-rule-loader").start();
        handler.removeCallbacks(check); handler.post(check);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if(!active() || event==null || event.getPackageName()==null) return;
        String pkg=event.getPackageName().toString();
        if(!ScreenRules.browser(pkg) && !ScreenRules.YOUTUBE.equals(pkg)) return;
        int eventWindowId=event.getWindowId();
        // Background content events must not replace the foreground target. A window
        // becomes trusted only through an allowlisted event and matching focus metadata.
        if(eventWindowId<0) return;
        if(eventWindowId!=lastWindowId || !pkg.equals(lastPackage)) {
            if(!focusedApplication(eventWindowId)) return;
            lastPackage=pkg; lastWindowId=eventWindowId; shortsBackPending=false;
        }
        // Coalesce frequent content-change events without retaining event objects or node trees.
        if(!queued) { queued=true; handler.postDelayed(eventCheck,80); }
    }
    private boolean queued;
    private final Runnable eventCheck=()-> { queued=false; inspect(); };
    private final Runnable check=new Runnable() { public void run() { if(stopped) return; inspect(); handler.postDelayed(this,500); } };
    private final Runnable coverFocusCheck=new Runnable() { public void run() {
        if(stopped || browserCover==null || !browserCover.visible()) return;
        // While covering a page, check window metadata more often than page contents.
        // No payment-package list, screen nodes or additional event scope is needed.
        if(!targetForeground(lastPackage,lastWindowId)) { hideBrowser(); return; }
        handler.postDelayed(this,100);
    } };
    private void inspect() {
        if(!active() || lastPackage.isEmpty() || CallSafety.callAudio(this)
            || !getSystemService(PowerManager.class).isInteractive() || getSystemService(KeyguardManager.class).isKeyguardLocked()) { hideBrowser(); return; }
        FocusStore store=new FocusStore(this);
        boolean youtube=ScreenRules.YOUTUBE.equals(lastPackage);
        if(youtube?store.shortsRemaining()==0:!store.enabled()) { shortsBackPending=false; hideBrowser(); return; }
        AccessibilityNodeInfo root=foregroundRoot();
        if(root==null) { hideBrowser(); status="Screen not exposed by this app"; return; }
        try {
            String pkg=root.getPackageName()==null?"":root.getPackageName().toString();
            if(!pkg.equals(lastPackage)) { lastPackage=""; lastWindowId=-1; shortsBackPending=false; hideBrowser(); return; }
            if(!youtube) {
                inspectBrowser(root,pkg,store);
                shortsBackPending=false;
                return;
            }
            hideBrowser();
            boolean block=false;
            if(youtube) { block=shortsVisible(root); status=block?"Shorts screen detected":"YouTube checked; no supported Shorts marker"; }
            if(!block) shortsBackPending=false;
            long now=SystemClock.elapsedRealtime();
            if(block && now-lastRedirect>=1000 && !CallSafety.callAudio(this) && root.refresh()) {
                // Recheck the timer at action time; browser domains are never logged or shown here.
                if(store.shortsRemaining()==0) return;
                AccessibilityNodeInfo current=foregroundRoot();
                if(current==null) return;
                try {
                    if(current.getPackageName()==null || !pkg.contentEquals(current.getPackageName()) || current.getWindowId()!=root.getWindowId()) return;
                    if(!shortsVisible(current) || !targetForeground(pkg,current.getWindowId())) return;
                }
                finally { current.recycle(); }
                // Leave Shorts within YouTube first, so ordinary videos remain reachable.
                // If Back did not dismiss the screen, the next checked attempt returns Home.
                boolean redirected;
                if(!shortsBackPending) {
                    shortsBackPending=true;
                    redirected=performGlobalAction(GLOBAL_ACTION_BACK);
                    if(!redirected) redirected=performGlobalAction(GLOBAL_ACTION_HOME);
                } else redirected=performGlobalAction(GLOBAL_ACTION_HOME);
                if(redirected) {
                    lastRedirect=now;
                    Toast.makeText(this,"Shorts are on a break",Toast.LENGTH_SHORT).show();
                }
            }
        } catch(RuntimeException unavailable) { hideBrowser(); status="Screen check unavailable; keep browser Secure DNS off"; }
        finally { root.recycle(); }
    }
    private void hideBrowser() { handler.removeCallbacks(coverFocusCheck); if(browserCover!=null) browserCover.hide(); coveredPage.clear(); }
    private boolean targetForeground(String pkg,int windowId) {
        return windowId>=0 && windowId==lastWindowId && pkg.equals(lastPackage)
            && (ScreenRules.browser(pkg) || ScreenRules.YOUTUBE.equals(pkg)) && focusedApplication(windowId);
    }
    private boolean focusedApplication(int windowId) {
        if(windowId<0) return false;
        List<AccessibilityWindowInfo> windows=Collections.emptyList();
        try { windows=getWindows(); return focusedTargetWindow(windows,windowId)!=null; }
        catch(RuntimeException unavailable) { return false; }
        finally { for(AccessibilityWindowInfo window:windows) window.recycle(); }
    }
    private AccessibilityWindowInfo focusedTargetWindow(List<AccessibilityWindowInfo> windows,int windowId) {
        // A non-focusable cover or the keyboard may be above the browser without
        // changing the focused application. System dialogs must still hide the cover.
        for(AccessibilityWindowInfo window:windows)
            if(window.isFocused() && window.getType()==AccessibilityWindowInfo.TYPE_SYSTEM) return null;
        for(AccessibilityWindowInfo window:windows) {
            if(window.getType()==AccessibilityWindowInfo.TYPE_APPLICATION && window.isFocused())
                return window.getId()==windowId?window:null;
        }
        return null;
    }
    private AccessibilityNodeInfo foregroundRoot() {
        if(lastWindowId<0 || (!ScreenRules.browser(lastPackage) && !ScreenRules.YOUTUBE.equals(lastPackage))) return null;
        List<AccessibilityWindowInfo> windows=Collections.emptyList();
        try {
            windows=getWindows();
            // Validate the allowlisted event's window ID BEFORE requesting its root.
            // Never fall back to the active root: it could now belong to a payment app.
            AccessibilityWindowInfo window=focusedTargetWindow(windows,lastWindowId);
            if(window==null) return null;
            AccessibilityNodeInfo root=window.getRoot();
            if(root==null) return null;
            if(root.getWindowId()!=lastWindowId || root.getPackageName()==null || !lastPackage.contentEquals(root.getPackageName())) {
                root.recycle(); return null;
            }
            return root;
        } catch(RuntimeException unavailable) { return null;
        } finally { for(AccessibilityWindowInfo window:windows) window.recycle(); }
    }
    private void inspectBrowser(AccessibilityNodeInfo root,String pkg,FocusStore store) {
        if(!ScreenRules.browser(pkg)) { hideBrowser(); return; }
        BrowserAddress snapshot=address(root,pkg);
        String value=snapshot.value;
        String host=ScreenRules.host(value);
        boolean blocked=!host.isEmpty() && WebsiteFilter.blocked(this,host);
        long remaining=store.webShortsRemaining();
        boolean shorts=!blocked && remaining>0 && ScreenRules.webShorts(value);
        Rect area=browserPageBounds(root);
        if(snapshot.editing && browserCover.visible() && !area.isEmpty() && coveredPage.keep(pkg,root.getWindowId(),SystemClock.elapsedRealtime())) {
            // Do not reveal the covered page merely because the user focuses the address bar.
            // The keyboard and browser controls remain outside this non-focusable cover.
            status="Page remains covered while editing its address"; return;
        }
        if(blocked || shorts) {
            if(area.isEmpty()) { hideBrowser(); status="Browser page bounds unavailable; DNS filtering remains active"; }
            else {
                long duration=shorts?remaining:0;
                if(blocked) {
                    if(WebsiteFilter.persistentBlock(host)) duration=Long.MAX_VALUE;
                    else for(TimedBlock timer:store.active()) if(!timer.app && !timer.shorts && DomainRules.covers(timer.target,host)) duration=Math.max(duration,store.remaining(timer));
                }
                if(duration<=0) { hideBrowser(); return; }
                if(!targetForeground(pkg,root.getWindowId())) { hideBrowser(); return; }
                coveredPage.cover(pkg,root.getWindowId(),duration,SystemClock.elapsedRealtime());
                browserCover.playback(ScreenRules.webShorts(value)?pauseWebShorts(root,pkg):"");
                if(!targetForeground(pkg,root.getWindowId())) { hideBrowser(); return; }
                browserCover.show(area,shorts,remaining); status="Blocked browser page covered; tab controls remain available";
                handler.removeCallbacks(coverFocusCheck); handler.postDelayed(coverFocusCheck,100);
            }
        } else { hideBrowser(); status=host.isEmpty()?"Browser address unavailable or being edited":WebsiteFilter.ready()?"Browser address checked":"Loading browser rules"; }
    }
    private String pauseWebShorts(AccessibilityNodeInfo root,String pkg) {
        // Only act on an explicitly labelled player button in a confirmed Shorts URL.
        // No global media keys/audio focus: unrelated playback and calls must stay untouched.
        AccessibilityNodeInfo page=webPageRoot(root);
        if(page==null) return "Player controls unavailable.";
        ArrayDeque<AccessibilityNodeInfo> queue=new ArrayDeque<>(); queue.add(page);
        int visited=0; boolean playVisible=false;
        try {
            while(!queue.isEmpty() && visited++<800) {
                AccessibilityNodeInfo node=queue.remove();
                try {
                    if(!node.isVisibleToUser() || node.isEditable() || node.isPassword()) continue;
                    if(node.isClickable() && ("android.widget.Button".contentEquals(node.getClassName()==null?"":node.getClassName()) || "android.widget.ImageButton".contentEquals(node.getClassName()==null?"":node.getClassName()))) {
                        String description=playerLabel(node);
                        if(ScreenRules.playControl(description)) playVisible=true;
                        if(ScreenRules.pauseControl(description)) {
                            if(SystemClock.elapsedRealtime()-lastMediaAttempt<1000) return "Playback pause requested.";
                            if(!node.refresh() || !ScreenRules.pauseControl(playerLabel(node))) continue;
                            if(!targetForeground(pkg,root.getWindowId()) || node.getWindowId()!=root.getWindowId() || !root.refresh()) return "Playback pause not confirmed.";
                            BrowserAddress current=address(root,pkg);
                            if(current.editing || !ScreenRules.webShorts(current.value) || CallSafety.callAudio(this)) return "Playback pause not confirmed.";
                            if(!targetForeground(pkg,root.getWindowId())) return "Playback pause not confirmed.";
                            lastMediaAttempt=SystemClock.elapsedRealtime();
                            if(node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return "Playback pause requested.";
                        }
                    }
                    for(int i=0;i<node.getChildCount() && queue.size()<800;i++) { AccessibilityNodeInfo child=node.getChild(i); if(child!=null) queue.add(child); }
                } finally { node.recycle(); }
            }
        } finally { while(!queue.isEmpty()) queue.remove().recycle(); }
        return playVisible?"Player shows paused.":"Pause control unavailable. If audio continues, pause it from the browser's media notification.";
    }
    private String playerLabel(AccessibilityNodeInfo button) {
        CharSequence label=button.getContentDescription();
        // Some Chromium versions expose an ARIA button label as text rather than description.
        if(label==null || label.length()==0) label=button.getText();
        return label==null?"":label.toString();
    }
    private AccessibilityNodeInfo webPageRoot(AccessibilityNodeInfo root) {
        ArrayDeque<AccessibilityNodeInfo> queue=new ArrayDeque<>(); queue.add(AccessibilityNodeInfo.obtain(root));
        int visited=0;
        try {
            while(!queue.isEmpty() && visited++<600) {
                AccessibilityNodeInfo node=queue.remove();
                try {
                    if(!node.isVisibleToUser()) continue;
                    if("android.webkit.WebView".contentEquals(node.getClassName()==null?"":node.getClassName())) return AccessibilityNodeInfo.obtain(node);
                    for(int i=0;i<node.getChildCount() && queue.size()<600;i++) { AccessibilityNodeInfo child=node.getChild(i); if(child!=null) queue.add(child); }
                } finally { node.recycle(); }
            }
        } finally { while(!queue.isEmpty()) queue.remove().recycle(); }
        return null;
    }
    private Rect browserPageBounds(AccessibilityNodeInfo root) {
        Rect window=new Rect(); root.getBoundsInScreen(window);
        Rect best=new Rect();
        ArrayDeque<AccessibilityNodeInfo> queue=new ArrayDeque<>(); queue.add(AccessibilityNodeInfo.obtain(root));
        int visited=0;
        try {
            while(!queue.isEmpty() && visited++<600) {
                AccessibilityNodeInfo node=queue.remove();
                try {
                    if(!node.isVisibleToUser()) continue;
                    CharSequence type=node.getClassName();
                    if("android.webkit.WebView".contentEquals(type==null?"":type)) {
                        Rect bounds=new Rect(); node.getBoundsInScreen(bounds);
                        if(bounds.intersect(window) && (long)bounds.width()*bounds.height()>(long)best.width()*best.height()) best.set(bounds);
                        // Never inspect HTML children, form fields or page text.
                        continue;
                    }
                    for(int i=0;i<node.getChildCount() && queue.size()<600;i++) { AccessibilityNodeInfo child=node.getChild(i); if(child!=null) queue.add(child); }
                } finally { node.recycle(); }
            }
        } finally { while(!queue.isEmpty()) queue.remove().recycle(); }
        return best;
    }
    private static final class BrowserAddress {
        String value="";
        boolean editing;
    }
    private BrowserAddress address(AccessibilityNodeInfo root,String pkg) {
        BrowserAddress result=new BrowserAddress();
        for(String prefix:new String[]{pkg,"com.brave.browser","com.chrome","org.chromium.chrome"}) {
            List<AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByViewId(prefix+":id/url_bar");
            String host="";
            for(AccessibilityNodeInfo node:nodes) {
                try {
                    if(node.isVisibleToUser() && node.isFocused()) result.editing=true;
                    if(host.isEmpty() && node.isVisibleToUser() && !node.isPassword() && !node.isFocused()) {
                        CharSequence value=node.getText();
                        if(value!=null && !ScreenRules.host(value.toString()).isEmpty()) host=value.toString();
                    }
                } finally { node.recycle(); }
            }
            if(!host.isEmpty()) { result.value=host; return result; }
        }
        return result;
    }
    private boolean shortsVisible(AccessibilityNodeInfo root) {
        ArrayDeque<AccessibilityNodeInfo> queue=new ArrayDeque<>(); queue.add(AccessibilityNodeInfo.obtain(root));
        int visited=0; boolean found=false;
        try {
            while(!queue.isEmpty() && visited++<600 && !found) {
                AccessibilityNodeInfo node=queue.remove();
                try {
                    if(!node.isVisibleToUser() || node.isPassword() || node.isEditable()) continue;
                    CharSequence description=node.isSelected()?node.getContentDescription():null;
                    found=ScreenRules.shortsMarker(node.getViewIdResourceName(),node.isSelected(),description==null?"":description.toString());
                    if(!found) for(int i=0;i<node.getChildCount() && queue.size()<600;i++) { AccessibilityNodeInfo child=node.getChild(i); if(child!=null) queue.add(child); }
                } finally { node.recycle(); }
            }
        } finally { while(!queue.isEmpty()) queue.remove().recycle(); }
        return found;
    }
    @Override public void onInterrupt() { lastPackage=""; lastWindowId=-1; hideBrowser(); }
    @Override public void onDestroy() { stopped=true; hideBrowser(); handler.removeCallbacksAndMessages(null); if(connected==this) connected=null; status="Screen-aware blocking disconnected"; super.onDestroy(); }
}
