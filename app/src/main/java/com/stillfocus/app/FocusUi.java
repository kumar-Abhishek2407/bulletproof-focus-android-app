package com.stillfocus.app;

import android.animation.ValueAnimator;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.Button;

/** Shared visual language. Motion follows Android's animator accessibility setting. */
public final class FocusUi {
    public static int ACCENT=0xff6554c0,INK=0xff202435,MUTED=0xff626a7c,PAPER=0xfff8f9fc;
    public static int LAVENDER=0xffeeeafb,PEACH=LAVENDER,MINT=0xffeaf5f1;
    public static int TEAL=0xff168575;
    public static int BUTTON=0xff6554c0,BUTTON_TEXT=0xffffffff;
    public static int CARD=0xffffffff,BORDER=0xffe3e6ee;
    public static boolean dark;
    public static boolean darkPreference(Context context) {
        return context.getSharedPreferences("appearance",0).getBoolean("dark",false);
    }
    public static void applyTheme(android.app.Activity activity) {
        dark=darkPreference(activity);
        ACCENT=dark?0xffc0b4ef:0xff6554c0;
        INK=dark?0xffe7e8ed:0xff202435;
        MUTED=dark?0xffa6abb7:0xff626a7c;
        PAPER=dark?0xff17191f:0xfff8f9fc;
        CARD=dark?0xff22252d:0xffffffff;
        BORDER=dark?0xff3b3f4a:0xffe3e6ee;
        LAVENDER=dark?0xff343044:0xffeeeafb; PEACH=LAVENDER;
        MINT=dark?0xff233a35:0xffeaf5f1;
        TEAL=dark?0xff82c9b7:0xff168575;
        BUTTON=dark?0xffb3a4e6:0xff6554c0;
        BUTTON_TEXT=dark?0xff211b35:0xffffffff;
        activity.setTheme(dark?R.style.AppThemeDark:R.style.AppTheme);
    }
    public static void toggleTheme(android.app.Activity activity) {
        activity.getSharedPreferences("appearance",0).edit().putBoolean("dark",!darkPreference(activity)).apply();
        (activity.getParent()!=null?activity.getParent():activity).recreate();
    }
    public static final long MOTION_MS=190;
    public static final PathInterpolator EASE=new PathInterpolator(.2f,0f,0f,1f);
    private FocusUi() { }
    private static int dp(Context c,int n) { return Math.round(n*c.getResources().getDisplayMetrics().density); }
    public static int palette(int color) {
        if(color==Color.WHITE) return CARD;
        if(color==0xffe8eee5 || color==0xffeaf0e9) return LAVENDER;
        if(color==0xffdfe9d9) return MINT;
        return color;
    }
    public static GradientDrawable surface(Context c,int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(palette(color)); d.setCornerRadius(dp(c,radius)); if(color==Color.WHITE) d.setStroke(dp(c,1),BORDER); return d;
    }
    public static GradientDrawable hero(Context c) {
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{PAPER,MINT});
        d.setCornerRadius(dp(c,28)); return d;
    }
    public static void button(Button b,boolean primary) {
        Context c=b.getContext(); b.setTextColor(primary?BUTTON_TEXT:ACCENT); b.setTextSize(16);
        b.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));
        GradientDrawable shape=surface(c,primary?BUTTON:PAPER,18);
        shape.setCornerRadius(dp(c,18)); if(!primary) shape.setStroke(dp(c,1),BORDER);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(primary?0x40ffffff:0x226554c0),shape,null));
        b.setStateListAnimator(AnimatorInflater.loadStateListAnimator(c,R.animator.button_press));
        b.setElevation(0); b.setMinHeight(dp(c,52));
    }
    public static void enter(View view,int direction) {
        if(direction==0 || !ValueAnimator.areAnimatorsEnabled()) return;
        int distance=dp(view.getContext(),20)*direction;
        view.animate().cancel(); view.setAlpha(.88f); view.setTranslationX(distance);
        view.animate().alpha(1f).translationX(0f).setDuration(MOTION_MS).setInterpolator(EASE).start();
    }
    public static void slidePages(android.widget.FrameLayout stage,View previous,View next,int direction) {
        if(previous==null || direction==0 || !ValueAnimator.areAnimatorsEnabled()) return;
        if(previous.getParent() instanceof android.view.ViewGroup) ((android.view.ViewGroup)previous.getParent()).removeView(previous);
        previous.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        stage.addView(previous,new android.widget.FrameLayout.LayoutParams(-1,-1));
        View touchShield=new View(stage.getContext()); touchShield.setClickable(true);
        touchShield.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        stage.addView(touchShield,new android.widget.FrameLayout.LayoutParams(-1,-1));
        next.setVisibility(View.INVISIBLE);
        stage.post(()-> {
            int width=stage.getWidth();
            next.setTranslationX(width*direction); next.setVisibility(View.VISIBLE);
            next.animate().translationX(0).setDuration(280).setInterpolator(EASE).start();
            previous.animate().translationX(-width*direction).setDuration(280).setInterpolator(EASE).withEndAction(()-> {
                stage.removeView(previous); stage.removeView(touchShield);
            }).start();
        });
    }
    public static ColorStateList selection() {
        return new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{ACCENT,MUTED});
    }
}
