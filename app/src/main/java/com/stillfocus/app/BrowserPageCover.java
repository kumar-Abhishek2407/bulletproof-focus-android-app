package com.stillfocus.app;

import android.accessibilityservice.AccessibilityService;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.view.*;
import android.widget.*;

/** Covers only browser content; never sends the browser Home or changes its tabs. */
final class BrowserPageCover {
    private final AccessibilityService service;
    private final WindowManager windows;
    private LinearLayout view;
    private final Rect shown=new Rect();
    private TextView detail;
    private String playback="";
    void playback(String value) { playback=value; }
    BrowserPageCover(AccessibilityService service) { this.service=service; windows=service.getSystemService(WindowManager.class); }
    boolean visible() { return view!=null; }
    void show(Rect area,boolean shorts,long remaining) {
        if(area.isEmpty()) { hide(); return; }
        if(view==null) {
            view=new LinearLayout(service); view.setOrientation(LinearLayout.VERTICAL); view.setGravity(Gravity.CENTER);
            int pad=Math.round(24*service.getResources().getDisplayMetrics().density);
            view.setPadding(pad,pad,pad,pad); view.setBackgroundColor(FocusUi.PAPER); view.setClickable(true);
            TextView title=new TextView(service); title.setText("This page is blocked"); title.setTextSize(24); title.setTextColor(FocusUi.INK); title.setGravity(Gravity.CENTER); view.addView(title);
            detail=new TextView(service); detail.setTextSize(16); detail.setTextColor(FocusUi.MUTED); detail.setGravity(Gravity.CENTER); detail.setPadding(0,pad,0,0); view.addView(detail);
            windows.addView(view,layout(area)); shown.set(area);
        } else if(!shown.equals(area)) { windows.updateViewLayout(view,layout(area)); shown.set(area); }
        String message=shorts?"Shorts are on a break. "+Math.max(1,(remaining+59999)/60000)+" min remaining.":"This website is restricted by Still Focus.";
        String value=message+(playback.isEmpty()?"":"\n\n"+playback)+"\n\nUse the address bar or switch tabs to keep browsing.";
        if(!value.contentEquals(detail.getText())) detail.setText(value);
    }
    private WindowManager.LayoutParams layout(Rect area) {
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(area.width(),area.height(),WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.OPAQUE);
        p.gravity=Gravity.TOP|Gravity.LEFT; p.x=area.left; p.y=area.top; p.setTitle("Still Focus blocked page"); return p;
    }
    void hide() {
        if(view!=null) { try { windows.removeViewImmediate(view); } catch(IllegalArgumentException ignored) { } }
        view=null; detail=null; playback=""; shown.setEmpty();
    }
}
