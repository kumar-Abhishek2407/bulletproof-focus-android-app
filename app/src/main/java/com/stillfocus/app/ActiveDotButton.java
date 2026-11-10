package com.stillfocus.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.widget.Button;

/** A small status marker; animation runs only while its window is visible and focused. */
public final class ActiveDotButton extends Button {
    private final Paint dotPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator pulse;
    private boolean active;
    private float phase=1f;
    public ActiveDotButton(Context context) {
        super(context);
        int space=Math.round(36*getResources().getDisplayMetrics().density);
        setPaddingRelative(space,getPaddingTop(),space,getPaddingBottom());
    }
    public void setActive(boolean value) {
        if(active==value) return;
        active=value;
        setContentDescription(getText()+(active?", items available":""));
        syncPulse(); invalidate();
    }
    private void syncPulse() {
        boolean run=active && isAttachedToWindow() && getWindowVisibility()==View.VISIBLE
                && hasWindowFocus() && ValueAnimator.areAnimatorsEnabled();
        if(!run) {
            if(pulse!=null) { pulse.cancel(); pulse=null; }
            phase=1f; invalidate(); return;
        }
        if(pulse!=null) return;
        pulse=ValueAnimator.ofFloat(0f,1f);
        pulse.setDuration(1100);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        pulse.addUpdateListener(a->{ phase=(float)a.getAnimatedValue(); invalidate(); });
        pulse.start();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(!active) return;
        float density=getResources().getDisplayMetrics().density;
        int color=(int)new android.animation.ArgbEvaluator().evaluate(phase,0xff367e70,0xff104f43);
        dotPaint.setColor(color); dotPaint.setAlpha(Math.round(255*(.6f+.4f*phase)));
        float x=getLayoutDirection()==View.LAYOUT_DIRECTION_RTL?20*density:getWidth()-20*density;
        canvas.drawCircle(x,getHeight()/2f,5*density*(.75f+.4f*phase),dotPaint);
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); syncPulse(); }
    @Override protected void onDetachedFromWindow() {
        if(pulse!=null) { pulse.cancel(); pulse=null; }
        super.onDetachedFromWindow();
    }
    @Override protected void onWindowVisibilityChanged(int visibility) { super.onWindowVisibilityChanged(visibility); syncPulse(); }
    @Override public void onWindowFocusChanged(boolean focused) { super.onWindowFocusChanged(focused); syncPulse(); }
}

