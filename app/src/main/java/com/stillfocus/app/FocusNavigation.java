package com.stillfocus.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.*;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.widget.*;

/** Four stable destinations shared by the focus, allowance and schedule screens. */
public final class FocusNavigation {
    private FocusNavigation() { }
    public static LinearLayout bar(Activity activity,int selected,java.util.function.IntConsumer select) {
        return bar(activity,selected,select,false);
    }
    public static LinearLayout bar(Activity activity,int selected,java.util.function.IntConsumer select,boolean expanded) {
        LinearLayout bar=new LinearLayout(activity); bar.setBackgroundColor(FocusUi.PAPER);
        float unit=activity.getResources().getDisplayMetrics().density;
        bar.setPadding((int)(4*unit),(int)(10*unit),(int)(4*unit),(int)(10*unit));
        String[] labels={"Focus","Daily limit","Schedule","Settings"};
        for(int n=0;n<labels.length;n++) {
            if(expanded && n!=selected) continue;
            final int index=n; TextView tab=new TextView(activity); tab.setText(expanded?"<  "+labels[n]:labels[n]); tab.setTextSize(expanded?17:n==selected?14:12);
            tab.setGravity(Gravity.CENTER); tab.setMinHeight((int)(56*unit)); tab.setPadding((int)(2*unit),(int)(8*unit),(int)(2*unit),(int)(8*unit));
            tab.setTextColor(n==selected?FocusUi.ACCENT:FocusUi.MUTED);
            tab.setTypeface(null,n==selected?Typeface.BOLD:Typeface.NORMAL);
            tab.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22b6e397),FocusUi.surface(activity,n==selected?FocusUi.LAVENDER:FocusUi.PAPER,14),null));
            tab.setSelected(n==selected); tab.setContentDescription(expanded?"Back to "+labels[n]:labels[n]+(n==selected?", selected":""));
            tab.setFocusable(true); tab.setOnClickListener(v->{ if(expanded || index!=selected) select.accept(index); });
            bar.addView(tab,new LinearLayout.LayoutParams(0,-2,n==selected?1.4f:1f));
            if(n==selected && ValueAnimator.areAnimatorsEnabled()) {
                tab.setScaleX(expanded?.96f:.98f); tab.setScaleY(.96f);
                tab.animate().scaleX(1f).scaleY(1f).setDuration(FocusUi.MOTION_MS).setInterpolator(FocusUi.EASE).start();
            }
        }
        return bar;
    }
    public static void open(Activity activity,int tab) {
        Class<?> target=tab==1?DailyLimitActivity.class:tab==2?ScheduleActivity.class:MainActivity.class;
        Intent intent=new Intent(activity,target).putExtra("navigationTab",tab);
        // Reuse the three tab activities rather than constructing whole screens for every tap.
        // Reordering keeps one instance of each and preserves selection/editor drafts.
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int current=activity instanceof DailyLimitActivity?1:activity instanceof ScheduleActivity?2:activity.getIntent().getIntExtra("navigationTab",0);
        activity.startActivity(intent);
        if(ValueAnimator.areAnimatorsEnabled()) activity.overridePendingTransition(tab>current?R.anim.tab_enter_right:R.anim.tab_enter_left,tab>current?R.anim.tab_exit_left:R.anim.tab_exit_right);
        else activity.overridePendingTransition(0,0);
    }
    /** Deliberate horizontal swipes only; wheels, text fields and system-edge gestures retain touch ownership. */
    public static final class Swipe {
        private float x,y;
        private boolean allowed;
        public int target(Activity activity,MotionEvent event,int selected) {
            float density=activity.getResources().getDisplayMetrics().density;
            View decor=activity.getWindow().getDecorView();
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN) {
                x=event.getX(); y=event.getY();
                allowed=x>24*density && x<decor.getWidth()-24*density && y<decor.getHeight()-96*density && !interactive(decor,event.getRawX(),event.getRawY());
            } else if(event.getActionMasked()==MotionEvent.ACTION_POINTER_DOWN || event.getActionMasked()==MotionEvent.ACTION_CANCEL) allowed=false;
            else if(event.getActionMasked()==MotionEvent.ACTION_MOVE && Math.abs(event.getY()-y)>48*density) allowed=false;
            else if(event.getActionMasked()==MotionEvent.ACTION_UP) {
                boolean accept=allowed; allowed=false;
                float dx=event.getX()-x,dy=event.getY()-y;
                // Follow the finger: swipe left to reveal the tab on the right.
                int next=selected+(dx<0?1:-1);
                if(accept && Math.abs(dx)>64*density && Math.abs(dx)>Math.abs(dy)*2 && next>=0 && next<4) return next;
            }
            return -1;
        }
        public void reset() { allowed=false; }
        private boolean interactive(View view,float x,float y) {
            if(view.getVisibility()!=View.VISIBLE) return false;
            int[] location=new int[2]; view.getLocationOnScreen(location);
            if(x<location[0] || y<location[1] || x>=location[0]+view.getWidth() || y>=location[1]+view.getHeight()) return false;
            if(view instanceof EditText || view instanceof NumberPicker || view instanceof SeekBar || view instanceof HorizontalScrollView) return true;
            if(view instanceof ViewGroup) { ViewGroup group=(ViewGroup)view; for(int i=0;i<group.getChildCount();i++) if(interactive(group.getChildAt(i),x,y)) return true; }
            return false;
        }
    }
}
