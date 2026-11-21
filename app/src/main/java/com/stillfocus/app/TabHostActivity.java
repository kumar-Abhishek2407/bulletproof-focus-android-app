package com.stillfocus.app;

import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.FrameLayout;
import android.animation.ValueAnimator;

/** Keeps real tab windows together so a drag can reveal both pages. */
@SuppressWarnings("deprecation")
public final class TabHostActivity extends ActivityGroup {
    private FrameLayout stage;
    private final View[] pages=new View[4];
    private int selected,neighbor=-1;
    private float downX,downY,offset;
    private boolean eligible,dragging,settling;
    private ValueAnimator animator;
    private VelocityTracker velocity;
    private Intent tabIntent(int tab) {
        return new Intent(this,tab==1?DailyLimitActivity.class:tab==2?ScheduleActivity.class:MainActivity.class)
            .putExtra("navigationTab",tab).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }
    private View page(int tab) {
        if(pages[tab]==null) {
            Window window=getLocalActivityManager().startActivity("tab"+tab,tabIntent(tab));
            pages[tab]=window.getDecorView();
            pages[tab].setBackgroundColor(FocusUi.PAPER);
        }
        return pages[tab];
    }
    private Activity child() { return getLocalActivityManager().getActivity("tab"+selected); }
    private boolean rootPage() {
        Activity a=child();
        return a instanceof MainActivity?((MainActivity)a).isTabRoot():
            a instanceof DailyLimitActivity?((DailyLimitActivity)a).isTabRoot():
            a instanceof ScheduleActivity && ((ScheduleActivity)a).isTabRoot();
    }
    @Override public void onCreate(Bundle state) {
        FocusUi.applyTheme(this); super.onCreate(state);
        selected=state==null?0:state.getInt("selected",0);
        stage=new FrameLayout(this); stage.setBackgroundColor(FocusUi.PAPER);
        FrameLayout shell=new FrameLayout(this); shell.addView(stage,new FrameLayout.LayoutParams(-1,-1));
        android.widget.ImageButton theme=new android.widget.ImageButton(this);
        theme.setImageResource(FocusUi.dark?R.drawable.ic_theme_sun:R.drawable.ic_theme_moon);
        theme.setImageTintList(android.content.res.ColorStateList.valueOf(FocusUi.ACCENT));
        theme.setBackground(FocusUi.surface(this,android.graphics.Color.WHITE,22));
        theme.setContentDescription(FocusUi.dark?"Switch to light mode":"Switch to dark mode");
        float density=getResources().getDisplayMetrics().density;
        int size=Math.round(48*density);
        FrameLayout.LayoutParams toggleParams=new FrameLayout.LayoutParams(size,size,Gravity.TOP|Gravity.END);
        toggleParams.rightMargin=Math.round(22*density); toggleParams.topMargin=Math.round(18*density);
        shell.addView(theme,toggleParams);
        theme.setOnClickListener(v->FocusUi.toggleTheme(this));
        shell.setOnApplyWindowInsetsListener((v,insets)-> {
            int top=android.os.Build.VERSION.SDK_INT>=30?insets.getInsets(WindowInsets.Type.systemBars()).top:insets.getSystemWindowInsetTop();
            toggleParams.topMargin=top+Math.round(18*density); theme.setLayoutParams(toggleParams);
            return insets;
        });
        setContentView(shell);
        attach(selected);
        if(android.os.Build.VERSION.SDK_INT>=33)
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::back);
        // Warm the neighboring root pages one at a time, outside the first frame.
        stage.postDelayed(()->warm(0),350);
    }
    private void warm(int tab) {
        if(isFinishing() || isDestroyed() || tab>=4) return;
        if(!hasWindowFocus() || !rootPage()) return;
        if(dragging || settling) { stage.postDelayed(()->warm(tab),250); return; }
        page(tab); activate();
        stage.postDelayed(()->warm(tab+1),100);
    }
    private void attach(int tab) {
        View view=page(tab);
        if(view.getParent()!=stage) {
            if(view.getParent() instanceof ViewGroup) ((ViewGroup)view.getParent()).removeView(view);
            stage.addView(view,new FrameLayout.LayoutParams(-1,-1));
        }
    }
    private void activate() {
        getLocalActivityManager().startActivity("tab"+selected,tabIntent(selected));
        View current=page(selected); current.setTranslationX(0); current.setAlpha(1);
        current.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    }
    public void select(int target,boolean animate) {
        if(target<0 || target>3 || target==selected || settling) return;
        neighbor=target; attach(target);
        pages[target].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        offset=0; drawPages(0);
        finishSlide(true,animate);
    }
    private void drawPages(float dx) {
        if(neighbor<0) return;
        float width=Math.max(1,stage.getWidth()),fraction=Math.min(1,Math.abs(dx)/width);
        offset=dx;
        pages[selected].setTranslationX(dx);
        pages[neighbor].setTranslationX(dx+(neighbor>selected?width:-width));
        boolean motion=ValueAnimator.areAnimatorsEnabled();
        pages[selected].setAlpha(motion?1f-.15f*fraction:1f);
        pages[neighbor].setAlpha(motion?.85f+.15f*fraction:1f);
    }
    private void finishSlide(boolean commit,boolean animate) {
        if(neighbor<0) return;
        settling=true;
        float end=commit?(neighbor>selected?-stage.getWidth():stage.getWidth()):0;
        if(!animate || !ValueAnimator.areAnimatorsEnabled()) { drawPages(end); complete(commit); return; }
        animator=ValueAnimator.ofFloat(offset,end);
        animator.setDuration(180);
        animator.setInterpolator(FocusUi.EASE);
        animator.addUpdateListener(a->drawPages((float)a.getAnimatedValue()));
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) { complete(commit); }
        });
        animator.start();
    }
    private void complete(boolean commit) {
        int old=selected,target=neighbor;
        if(target<0) return;
        if(commit) selected=target;
        int removed=commit?old:target;
        stage.removeView(pages[removed]); pages[removed].setTranslationX(0); pages[removed].setAlpha(1);
        neighbor=-1; dragging=false; settling=false; animator=null; offset=0;
        activate();
    }
    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        if(settling) return true;
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN) {
            downX=e.getX(); downY=e.getY(); float d=getResources().getDisplayMetrics().density;
            eligible=rootPage() && downX>24*d && downX<stage.getWidth()-24*d && downY<stage.getHeight()-100*d
                && !interactive(pages[selected],e.getRawX(),e.getRawY());
            if(velocity!=null) velocity.recycle();
            velocity=VelocityTracker.obtain(); velocity.addMovement(e);
        } else if(velocity!=null) velocity.addMovement(e);
        if(action==MotionEvent.ACTION_MOVE && eligible) {
            float dx=e.getX()-downX,dy=e.getY()-downY;
            int slop=ViewConfiguration.get(this).getScaledTouchSlop();
            if(!dragging && Math.abs(dy)>slop && Math.abs(dy)>Math.abs(dx)) eligible=false;
            if(!dragging && Math.abs(dx)>slop && Math.abs(dx)>Math.abs(dy)*1.5f) {
                int next=selected+(dx<0?1:-1);
                if(next<0 || next>3) { eligible=false; return super.dispatchTouchEvent(e); }
                neighbor=next; attach(next); dragging=true;
                pages[next].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                MotionEvent cancel=MotionEvent.obtain(e); cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel); cancel.recycle();
            }
            if(dragging) { dx=neighbor>selected?Math.min(0,dx):Math.max(0,dx); drawPages(dx); return true; }
        }
        if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL || action==MotionEvent.ACTION_POINTER_DOWN) {
            eligible=false;
            if(dragging) {
                float speed=0; if(velocity!=null) { velocity.computeCurrentVelocity(1000); speed=velocity.getXVelocity(); }
                boolean forward=neighbor>selected?speed<0:speed>0;
                boolean commit=action==MotionEvent.ACTION_UP && (Math.abs(offset)>stage.getWidth()*.35f ||
                    forward && Math.abs(speed)>800*getResources().getDisplayMetrics().density);
                if(velocity!=null) { velocity.recycle(); velocity=null; }
                finishSlide(commit,true); return true;
            }
            if(velocity!=null) { velocity.recycle(); velocity=null; }
        }
        return super.dispatchTouchEvent(e);
    }
    private boolean interactive(View view,float x,float y) {
        if(view==null || view.getVisibility()!=View.VISIBLE) return false;
        int[] position=new int[2]; view.getLocationOnScreen(position);
        if(x<position[0] || y<position[1] || x>=position[0]+view.getWidth() || y>=position[1]+view.getHeight()) return false;
        if(view instanceof android.widget.EditText || view instanceof android.widget.NumberPicker ||
            view instanceof android.widget.SeekBar || view instanceof android.widget.HorizontalScrollView) return true;
        if(view instanceof ViewGroup) { ViewGroup group=(ViewGroup)view; for(int i=0;i<group.getChildCount();i++) if(interactive(group.getChildAt(i),x,y)) return true; }
        return false;
    }
    private void back() {
        if(settling) return;
        if(rootPage()) { if(selected==0) moveTaskToBack(true); else select(0,true); }
        else if(child()!=null) child().onBackPressed();
    }
    @Override public void onBackPressed() { back(); }
    @Override protected void onSaveInstanceState(Bundle state) { state.putInt("selected",selected); super.onSaveInstanceState(state); }
    @Override protected void onPause() {
        if(animator!=null) animator.end();
        if(dragging && neighbor>=0) complete(false);
        eligible=false;
        if(velocity!=null) { velocity.recycle(); velocity=null; }
        super.onPause();
    }
}
