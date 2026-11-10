from pathlib import Path
base=Path("app/src/main/java/com/stillfocus/app")
(base/"ActiveDotButton.java").write_text("""package com.stillfocus.app;

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
        int color=(int)android.animation.ArgbEvaluator.getInstance().evaluate(phase,0xff367e70,0xff104f43);
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
""",encoding="utf-8")
p=base/"MainActivity.java";s=p.read_text(encoding="utf-8")
s=s.replace('    private Button button(String title,boolean primary,Runnable action) {','    private ActiveDotButton activeBlockersButton;\n    private Button button(String title,boolean primary,Runnable action) {')
s=s.replace('Button b=new Button(this); b.setText(title);','Button b=title.equals("Active blockers")?new ActiveDotButton(this):new Button(this); b.setText(title);',1)
s=s.replace('root.addView(button("Active blockers",false,()->navigate(Page.SESSION)));','activeBlockersButton=(ActiveDotButton)button("Active blockers",false,()->navigate(Page.SESSION));\n        activeBlockersButton.setPaddingRelative(dp(36),dp(10),dp(36),dp(10));\n        root.addView(activeBlockersButton);')
s=s.replace('    private void updateLiveViews() {','    private void updateLiveViews() {\n        if(activeBlockersButton!=null) activeBlockersButton.setActive(!store.active().isEmpty());')
p.write_text(s,encoding="utf-8")
p=base/"DailyLimitActivity.java";s=p.read_text(encoding="utf-8")
s=s.replace('    private DailyLimitStore store;','    private DailyLimitStore store;\n    private ActiveDotButton limitsButton;')
s=s.replace('Button b=new Button(this); b.setText(label);','Button b=label.equals("Apps with limits")?new ActiveDotButton(this):new Button(this); b.setText(label);',1)
s=s.replace('root.addView(button("Apps with limits",()->openList(true)));','limitsButton=(ActiveDotButton)button("Apps with limits",()->openList(true)); root.addView(limitsButton);\n        limitsButton.setActive(!store.limits().isEmpty());')
s=s.replace('    private void update() {','    private void update() {\n        if(limitsButton!=null) limitsButton.setActive(!store.limits().isEmpty());')
p.write_text(s,encoding="utf-8")
p=Path("README.md");s=p.read_text(encoding="utf-8").replace('- Active-dot styles are [available for preview](artifacts/ui-preview/active-dot-examples.html). They are not enabled in this build pending approval.','- The approved Mint breathe dot appears on Active blockers when timers are active and Apps with limits when limits are saved. It gently changes colour and size, stops when the window loses focus, and respects disabled animations. [Compare the original previews](artifacts/ui-preview/active-dot-examples.html).')
p.write_text(s,encoding="utf-8")
