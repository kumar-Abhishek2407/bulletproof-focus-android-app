package com.stillfocus.app;

import android.animation.ValueAnimator;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;

/** Shared visual language. Motion follows Android's animator accessibility setting. */
public final class FocusUi {
    public static final int ACCENT=0xff6750c4,INK=0xff292342,MUTED=0xff6c647e,PAPER=0xfffaf8ff;
    public static final int LAVENDER=0xffeee7ff,PEACH=LAVENDER,MINT=0xffdcf4ec;
    private FocusUi() { }
    private static int dp(Context c,int n) { return Math.round(n*c.getResources().getDisplayMetrics().density); }
    public static int palette(int color) {
        if(color==0xffe8eee5 || color==0xffeaf0e9) return LAVENDER;
        if(color==0xffdfe9d9) return MINT;
        return color;
    }
    public static GradientDrawable surface(Context c,int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(palette(color)); d.setCornerRadius(dp(c,radius)); return d;
    }
    public static GradientDrawable hero(Context c) {
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xffd9ccff,0xfff5dff1});
        d.setCornerRadius(dp(c,28)); return d;
    }
    public static void button(Button b,boolean primary) {
        Context c=b.getContext(); b.setTextColor(primary?Color.WHITE:ACCENT); b.setTextSize(16);
        b.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));
        GradientDrawable shape=surface(c,primary?ACCENT:LAVENDER,18);
        shape.setCornerRadius(dp(c,18));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(primary?0x40ffffff:0x226750c4),shape,null));
        b.setStateListAnimator(AnimatorInflater.loadStateListAnimator(c,R.animator.button_press));
        b.setElevation(dp(c,primary?2:0)); b.setMinHeight(dp(c,52));
    }
    public static void enter(View view,int direction) {
        if(direction==0 || !ValueAnimator.areAnimatorsEnabled()) return;
        int distance=dp(view.getContext(),24)*direction;
        view.setAlpha(0f); view.setTranslationX(distance);
        view.post(()->view.animate().alpha(1f).translationX(0f).setDuration(240).setInterpolator(new DecelerateInterpolator()).start());
    }
    public static ColorStateList selection() {
        return new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{ACCENT,MUTED});
    }
}
