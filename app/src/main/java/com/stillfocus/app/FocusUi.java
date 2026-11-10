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
    public static final int ACCENT=0xffb6e397,INK=0xfff0f4ec,MUTED=0xffb5bdb2,PAPER=0xff131914;
    public static final int LAVENDER=0xff252e27,PEACH=LAVENDER,MINT=0xff24372b;
    public static final int TEAL=0xff8de0ce;
    public static final int BUTTON=0xff8de0ce,BUTTON_TEXT=0xff102b26;
    public static final long MOTION_MS=190;
    public static final PathInterpolator EASE=new PathInterpolator(.2f,0f,0f,1f);
    private FocusUi() { }
    private static int dp(Context c,int n) { return Math.round(n*c.getResources().getDisplayMetrics().density); }
    public static int palette(int color) {
        if(color==Color.WHITE) return 0xff1d261f;
        if(color==0xffe8eee5 || color==0xffeaf0e9) return LAVENDER;
        if(color==0xffdfe9d9) return MINT;
        return color;
    }
    public static GradientDrawable surface(Context c,int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(palette(color)); d.setCornerRadius(dp(c,radius)); return d;
    }
    public static GradientDrawable hero(Context c) {
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{PAPER,MINT});
        d.setCornerRadius(dp(c,28)); return d;
    }
    public static void button(Button b,boolean primary) {
        Context c=b.getContext(); b.setTextColor(BUTTON_TEXT); b.setTextSize(16);
        b.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));
        GradientDrawable shape=surface(c,BUTTON,18);
        shape.setCornerRadius(dp(c,18));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(primary?0x40ffffff:0x22b6e397),shape,null));
        b.setStateListAnimator(AnimatorInflater.loadStateListAnimator(c,R.animator.button_press));
        b.setElevation(dp(c,primary?2:0)); b.setMinHeight(dp(c,52));
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
